package com.capstone.smart_parcel.service;

import com.capstone.smart_parcel.domain.Chute;
import com.capstone.smart_parcel.domain.RuleChute;
import com.capstone.smart_parcel.domain.SortingGroup;
import com.capstone.smart_parcel.domain.SortingRule;
import com.capstone.smart_parcel.domain.enums.InputType;
import com.capstone.smart_parcel.dto.common.PageResponse;
import com.capstone.smart_parcel.dto.sorting.SortingRuleCreateRequest;
import com.capstone.smart_parcel.dto.sorting.SortingRuleResponse;
import com.capstone.smart_parcel.dto.sorting.SortingRuleUpdateRequest;
import com.capstone.smart_parcel.repository.ChuteRepository;
import com.capstone.smart_parcel.repository.RuleChuteRepository;
import com.capstone.smart_parcel.repository.SortingGroupRepository;
import com.capstone.smart_parcel.repository.SortingRuleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class SortingRuleService {

    private final SortingRuleRepository sortingRuleRepository;
    private final SortingGroupRepository sortingGroupRepository;
    private final RuleChuteRepository ruleChuteRepository;
    private final ChuteRepository chuteRepository;
    private final SortingContextService sortingContextService;

    @Transactional(readOnly = true)
    public PageResponse<SortingRuleResponse> getRules(String email,
                                                      Long groupId,
                                                      InputType type,
                                                      String keyword,
                                                      Pageable pageable) {
        var ctx = sortingContextService.resolve(email);
        ensureGroupAccessible(groupId, ctx.manager().getId());

        String normalized = normalizeKeyword(keyword);
        String pattern = normalized == null ? null : "%" + normalized + "%";

        Page<SortingRule> page = sortingRuleRepository.searchByGroupAndManager(
                ctx.manager().getId(),
                groupId,
                type,
                pattern,
                pageable
        );

        Map<Long, List<RuleChute>> assignments = fetchAssignments(page.getContent());

        return PageResponse.of(page, rule ->
                SortingRuleResponse.from(rule, toSummaries(assignments.get(rule.getId()))));
    }

    @Transactional
    public SortingRuleResponse createRule(String email,
                                          Long groupId,
                                          SortingRuleCreateRequest request) {
        var ctx = sortingContextService.resolve(email);
        sortingContextService.ensureManager(ctx.actor());

        SortingGroup group = ensureGroupAccessible(groupId, ctx.manager().getId());

        String ruleName = request.getRuleName().trim();
        if (sortingRuleRepository.existsByGroup_IdAndRuleName(group.getId(), ruleName)) {
            throw new IllegalArgumentException("Rule name already exists.");
        }

        SortingRule rule = new SortingRule();
        rule.setGroup(group);
        rule.setRuleName(ruleName);
        rule.setInputType(request.getInputType());
        rule.setInputValue(request.getInputValue().trim());
        rule.setItemName(request.getItemName().trim());
        rule.setCreatedAt(OffsetDateTime.now());

        sortingRuleRepository.save(rule);
        group.setUpdatedAt(OffsetDateTime.now());

        List<Long> chuteIds = normalizeChuteIds(request.getChuteIds());
        List<SortingRuleResponse.ChuteSummary> chutes = assignChutes(rule, chuteIds);

        return SortingRuleResponse.from(rule, chutes);
    }

    @Transactional
    public SortingRuleResponse updateRule(String email,
                                          Long ruleId,
                                          SortingRuleUpdateRequest request) {
        var ctx = sortingContextService.resolve(email);
        sortingContextService.ensureManager(ctx.actor());

        SortingRule rule = sortingRuleRepository.findById(ruleId)
                .orElseThrow(() -> new NoSuchElementException("Sorting rule not found."));

        SortingGroup group = rule.getGroup();
        if (group == null || !Objects.equals(group.getManager().getId(), ctx.manager().getId())) {
            throw new IllegalArgumentException("Sorting rule is not accessible.");
        }

        if (request.getRuleName() != null) {
            String name = request.getRuleName().trim();
            if (name.isEmpty()) {
                throw new IllegalArgumentException("Rule name must not be blank.");
            }
            if (!name.equals(rule.getRuleName())
                    && sortingRuleRepository.existsByGroup_IdAndRuleName(group.getId(), name)) {
                throw new IllegalArgumentException("Rule name already exists.");
            }
            rule.setRuleName(name);
        }

        if (request.getInputType() != null) {
            rule.setInputType(request.getInputType());
        }

        if (request.getInputValue() != null) {
            String value = request.getInputValue().trim();
            if (value.isEmpty()) {
                throw new IllegalArgumentException("Input value must not be blank.");
            }
            rule.setInputValue(value);
        }

        if (request.getItemName() != null) {
            String itemName = request.getItemName().trim();
            if (itemName.isEmpty()) {
                throw new IllegalArgumentException("Item name must not be blank.");
            }
            rule.setItemName(itemName);
        }

        List<SortingRuleResponse.ChuteSummary> chuteSummaries;
        if (request.getChuteIds() != null) {
            List<Long> chuteIds = normalizeChuteIds(request.getChuteIds());
            ruleChuteRepository.deleteAllByRuleId(ruleId);
            chuteSummaries = assignChutes(rule, chuteIds);
        } else {
            chuteSummaries = toSummaries(ruleChuteRepository.findByRule_Id(ruleId));
        }

        group.setUpdatedAt(OffsetDateTime.now());
        return SortingRuleResponse.from(rule, chuteSummaries);
    }

    @Transactional
    public void deleteRule(String email, Long ruleId) {
        var ctx = sortingContextService.resolve(email);
        sortingContextService.ensureManager(ctx.actor());

        SortingRule rule = sortingRuleRepository.findById(ruleId)
                .orElseThrow(() -> new NoSuchElementException("Sorting rule not found."));

        SortingGroup group = rule.getGroup();
        if (group == null || !Objects.equals(group.getManager().getId(), ctx.manager().getId())) {
            throw new IllegalArgumentException("Sorting rule is not accessible.");
        }

        ruleChuteRepository.deleteAllByRuleId(ruleId);
        sortingRuleRepository.delete(rule);
        group.setUpdatedAt(OffsetDateTime.now());
    }

    private SortingGroup ensureGroupAccessible(Long groupId, Long managerId) {
        return sortingGroupRepository.findByIdAndManager_Id(groupId, managerId)
                .orElseThrow(() -> new NoSuchElementException("Sorting group not found."));
    }

    private Map<Long, List<RuleChute>> fetchAssignments(List<SortingRule> rules) {
        if (rules.isEmpty()) {
            return Collections.emptyMap();
        }
        List<Long> ruleIds = rules.stream()
                .map(SortingRule::getId)
                .filter(Objects::nonNull)
                .toList();
        if (ruleIds.isEmpty()) {
            return Collections.emptyMap();
        }
        return ruleChuteRepository.findByRule_IdIn(ruleIds).stream()
                .collect(Collectors.groupingBy(rc -> rc.getRule().getId()));
    }

    private List<SortingRuleResponse.ChuteSummary> assignChutes(SortingRule rule, List<Long> chuteIds) {
        if (chuteIds.isEmpty()) {
            return List.of();
        }

        List<Chute> chutes = chuteRepository.findAllById(chuteIds);
        if (chutes.size() != chuteIds.size()) {
            throw new IllegalArgumentException("Invalid chute id is included.");
        }

        List<RuleChute> entities = chutes.stream()
                .map(chute -> RuleChute.builder()
                        .rule(rule)
                        .chute(chute)
                        .build())
                .toList();

        ruleChuteRepository.saveAll(entities);
        return toSummaries(entities);
    }

    private List<SortingRuleResponse.ChuteSummary> toSummaries(List<RuleChute> ruleChutes) {
        if (ruleChutes == null || ruleChutes.isEmpty()) {
            return List.of();
        }
        return ruleChutes.stream()
                .map(rc -> {
                    Chute chute = rc.getChute();
                    return new SortingRuleResponse.ChuteSummary(
                            chute.getId(),
                            chute.getChuteName(),
                            chute.getServoDeg()
                    );
                })
                .toList();
    }

    private String normalizeKeyword(String keyword) {
        if (keyword == null) {
            return null;
        }
        String value = keyword.trim();
        if (value.isEmpty()) {
            return null;
        }
        return value.toLowerCase(Locale.ROOT);
    }

    private List<Long> normalizeChuteIds(List<Long> chuteIds) {
        if (chuteIds == null) {
            return List.of();
        }
        List<Long> normalized = chuteIds.stream()
                .filter(Objects::nonNull)
                .distinct()
                .toList();
        if (normalized.size() > 1) {
            throw new IllegalArgumentException("각 분류 규칙에는 하나의 분류 라인만 지정할 수 있습니다.");
        }
        return normalized;
    }
}

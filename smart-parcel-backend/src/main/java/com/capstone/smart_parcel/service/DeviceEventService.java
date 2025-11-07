package com.capstone.smart_parcel.service;

import com.capstone.smart_parcel.domain.Chute;
import com.capstone.smart_parcel.domain.ErrorLog;
import com.capstone.smart_parcel.domain.RuleChute;
import com.capstone.smart_parcel.domain.SortingGroup;
import com.capstone.smart_parcel.domain.SortingHistory;
import com.capstone.smart_parcel.domain.SortingRule;
import com.capstone.smart_parcel.domain.User;
import com.capstone.smart_parcel.domain.enums.Role;
import com.capstone.smart_parcel.dto.device.DeviceErrorEventRequest;
import com.capstone.smart_parcel.dto.device.DeviceSetupResponse;
import com.capstone.smart_parcel.dto.device.DeviceSortingResultRequest;
import com.capstone.smart_parcel.repository.ChuteRepository;
import com.capstone.smart_parcel.repository.ErrorLogRepository;
import com.capstone.smart_parcel.repository.RuleChuteRepository;
import com.capstone.smart_parcel.repository.SortingGroupRepository;
import com.capstone.smart_parcel.repository.SortingHistoryRepository;
import com.capstone.smart_parcel.repository.SortingRuleRepository;
import com.capstone.smart_parcel.repository.UserRepository;
import com.capstone.smart_parcel.service.support.ImageStorageService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class DeviceEventService {

    private final UserRepository userRepository;
    private final SortingGroupRepository sortingGroupRepository;
    private final SortingRuleRepository sortingRuleRepository;
    private final RuleChuteRepository ruleChuteRepository;
    private final ChuteRepository chuteRepository;
    private final SortingHistoryRepository sortingHistoryRepository;
    private final ErrorLogRepository errorLogRepository;
    private final ImageStorageService imageStorageService;
    private final NotificationService notificationService;

    @Transactional(readOnly = true)
    public DeviceSetupResponse fetchSetup(Long managerId) {
        User manager = fetchManager(managerId);
        SortingGroup group = sortingGroupRepository.findFirstByManager_IdAndEnabledTrue(manager.getId())
                .orElseThrow(() -> new NoSuchElementException("활성화된 분류 그룹이 없습니다."));

        List<SortingRule> rules = sortingRuleRepository.findByGroup_Id(group.getId());
        Map<Long, List<RuleChute>> assignments = rules.isEmpty()
                ? Map.of()
                : ruleChuteRepository.findByRule_IdIn(rules.stream().map(SortingRule::getId).toList())
                .stream()
                .collect(Collectors.groupingBy(rc -> rc.getRule().getId()));

        return DeviceSetupResponse.from(manager, group, rules, assignments);
    }

    @Transactional
    public Long recordSortingResult(DeviceSortingResultRequest request, MultipartFile image) {
        if (image == null || image.isEmpty()) {
            throw new IllegalArgumentException("이미지 파일이 필요합니다.");
        }

        User manager = fetchManager(request.managerId());
        SortingRule rule = sortingRuleRepository.findById(request.ruleId())
                .orElseThrow(() -> new NoSuchElementException("분류 규칙을 찾을 수 없습니다."));
        SortingGroup group = rule.getGroup();
        if (group == null || !Objects.equals(group.getManager().getId(), manager.getId())) {
            throw new IllegalArgumentException("요청한 규칙이 해당 관리자에 속하지 않습니다.");
        }

        Chute chute = chuteRepository.findById(request.chuteId())
                .orElseThrow(() -> new NoSuchElementException("슈트를 찾을 수 없습니다."));
        if (!ruleChuteRepository.existsByRule_IdAndChute_Id(rule.getId(), chute.getId())) {
            throw new IllegalArgumentException("규칙과 슈트 매핑이 일치하지 않습니다.");
        }

        String storedPath = imageStorageService.store(image, "sorting-results", manager.getId().toString());

        SortingHistory history = SortingHistory.builder()
                .imageUrl(storedPath)
                .processedAt(request.processedAt() != null ? request.processedAt() : OffsetDateTime.now())
                .sortingGroupNameSnapshot(resolveItemName(rule, group))
                .chuteNameSnapshot(chute.getChuteName())
                .manager(manager)
                .group(group)
                .chute(chute)
                .build();

        sortingHistoryRepository.save(history);
        return history.getId();
    }

    @Transactional
    public Long recordErrorEvent(DeviceErrorEventRequest request, MultipartFile image) {
        User manager = fetchManager(request.managerId());

        SortingGroup group = null;
        if (request.groupId() != null) {
            group = sortingGroupRepository.findByIdAndManager_Id(request.groupId(), manager.getId())
                    .orElseThrow(() -> new IllegalArgumentException("요청한 그룹을 찾을 수 없습니다."));
        }

        SortingRule rule = null;
        if (request.ruleId() != null) {
            rule = sortingRuleRepository.findById(request.ruleId())
                    .orElseThrow(() -> new NoSuchElementException("분류 규칙을 찾을 수 없습니다."));
            if (rule.getGroup() == null || !Objects.equals(rule.getGroup().getManager().getId(), manager.getId())) {
                throw new IllegalArgumentException("요청한 규칙이 해당 관리자에 속하지 않습니다.");
            }
            if (group == null) {
                group = rule.getGroup();
            } else if (!Objects.equals(group.getId(), rule.getGroup().getId())) {
                throw new IllegalArgumentException("그룹과 규칙 정보가 일치하지 않습니다.");
            }
        }

        Chute chute = null;
        if (request.chuteId() != null) {
            chute = chuteRepository.findById(request.chuteId())
                    .orElseThrow(() -> new NoSuchElementException("슈트를 찾을 수 없습니다."));
            if (rule != null && !ruleChuteRepository.existsByRule_IdAndChute_Id(rule.getId(), chute.getId())) {
                throw new IllegalArgumentException("규칙과 슈트 매핑이 일치하지 않습니다.");
            }
        }

        String storedPath = null;
        if (image != null && !image.isEmpty()) {
            storedPath = imageStorageService.store(image, "error-events", manager.getId().toString());
        }

        ErrorLog errorLog = ErrorLog.builder()
                .errorCode(request.errorCode().trim())
                .occurredAt(request.occurredAt() != null ? request.occurredAt() : OffsetDateTime.now())
                .imageUrl(storedPath)
                .sortingGroupNameSnapshot(group != null ? group.getGroupName() : null)
                .chuteNameSnapshot(chute != null ? chute.getChuteName() : null)
                .manager(manager)
                .group(group)
                .chute(chute)
                .build();

        errorLogRepository.save(errorLog);
        notificationService.notifyError(errorLog);
        return errorLog.getId();
    }

    private User fetchManager(Long managerId) {
        User user = userRepository.findById(managerId)
                .orElseThrow(() -> new NoSuchElementException("관리자 계정을 찾을 수 없습니다."));
        if (user.getRole() != Role.MANAGER) {
            throw new IllegalArgumentException("해당 계정은 관리자 권한이 아닙니다.");
        }
        return user;
    }

    private String resolveItemName(SortingRule rule, SortingGroup group) {
        String itemName = rule.getItemName();
        if (itemName != null && !itemName.isBlank()) {
            return itemName;
        }
        return group.getGroupName();
    }
}

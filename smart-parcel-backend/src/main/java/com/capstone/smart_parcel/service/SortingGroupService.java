package com.capstone.smart_parcel.service;

import com.capstone.smart_parcel.domain.SortingGroup;
import com.capstone.smart_parcel.dto.common.PageResponse;
import com.capstone.smart_parcel.dto.sorting.*;
import com.capstone.smart_parcel.repository.SortingGroupRepository;
import com.capstone.smart_parcel.repository.SortingHistoryRepository;
import com.capstone.smart_parcel.repository.projection.GroupProcessingCountView;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class SortingGroupService {

    private final SortingGroupRepository sortingGroupRepository;
    private final SortingHistoryRepository sortingHistoryRepository;
    private final SortingContextService sortingContextService;

    @Transactional(readOnly = true)
    public PageResponse<SortingGroupResponse> getGroups(String email, String q, Boolean enabled, Pageable pageable) {
        var ctx = sortingContextService.resolve(email);
        String keyword = normalizeKeyword(q);
        String keywordPattern = keyword == null ? null : "%" + keyword + "%";
        Page<SortingGroup> page = sortingGroupRepository.searchByManager(
                ctx.manager().getId(),
                keywordPattern,
                enabled,
                pageable
        );
        var counts = fetchProcessingCounts(ctx.manager().getId(), page.getContent());

        return PageResponse.of(page, group ->
                SortingGroupResponse.from(group, counts.getOrDefault(group.getId(), 0L)));
    }

    @Transactional
    public SortingGroupResponse createGroup(String email, SortingGroupCreateRequest request) {
        var ctx = sortingContextService.resolve(email);
        sortingContextService.ensureManager(ctx.actor());

        SortingGroup group = new SortingGroup();
        group.setGroupName(request.getGroupName().trim());
        group.setManager(ctx.manager());
        group.setEnabled(false);
        group.setUpdatedAt(OffsetDateTime.now());

        sortingGroupRepository.save(group);
        return SortingGroupResponse.from(group);
    }

    @Transactional
    public SortingGroupResponse updateGroup(String email, Long groupId, SortingGroupUpdateRequest request) {
        var ctx = sortingContextService.resolve(email);
        sortingContextService.ensureManager(ctx.actor());

        SortingGroup group = sortingGroupRepository.findByIdAndManager_Id(groupId, ctx.manager().getId())
                .orElseThrow(() -> new NoSuchElementException("분류 그룹을 찾을 수 없습니다."));

        group.setGroupName(request.getGroupName().trim());
        group.setUpdatedAt(OffsetDateTime.now());

        return SortingGroupResponse.from(group);
    }

    @Transactional
    public void deleteGroup(String email, Long groupId) {
        var ctx = sortingContextService.resolve(email);
        sortingContextService.ensureManager(ctx.actor());

        SortingGroup group = sortingGroupRepository.findByIdAndManager_Id(groupId, ctx.manager().getId())
                .orElseThrow(() -> new NoSuchElementException("분류 그룹을 찾을 수 없습니다."));

        sortingGroupRepository.delete(group);
    }

    @Transactional
    public void enableGroup(String email, Long groupId) {
        var ctx = sortingContextService.resolve(email);
        sortingContextService.ensureManager(ctx.actor());

        SortingGroup group = sortingGroupRepository.findByIdAndManager_Id(groupId, ctx.manager().getId())
                .orElseThrow(() -> new NoSuchElementException("분류 그룹을 찾을 수 없습니다."));

        sortingGroupRepository.disableAll();
        sortingGroupRepository.enable(groupId);
        group.setEnabled(true);
        group.setUpdatedAt(OffsetDateTime.now());
    }

    @Transactional
    public void disableGroup(String email, Long groupId) {
        var ctx = sortingContextService.resolve(email);
        sortingContextService.ensureManager(ctx.actor());

        SortingGroup group = sortingGroupRepository.findByIdAndManager_Id(groupId, ctx.manager().getId())
                .orElseThrow(() -> new NoSuchElementException("분류 그룹을 찾을 수 없습니다."));

        sortingGroupRepository.disable(groupId);
        group.setEnabled(false);
        group.setUpdatedAt(OffsetDateTime.now());
    }

    private String normalizeKeyword(String keyword) {
        if (keyword == null) return null;
        String v = keyword.trim();
        if (v.isEmpty()) return null;
        return v.toLowerCase(Locale.ROOT);
    }

    private Map<Long, Long> fetchProcessingCounts(Long managerId, List<SortingGroup> groups) {
        if (groups.isEmpty()) {
            return Map.of();
        }
        List<Long> groupIds = groups.stream()
                .map(SortingGroup::getId)
                .toList();

        List<GroupProcessingCountView> views = sortingHistoryRepository.countByGroupIds(managerId, groupIds);
        return views.stream()
                .collect(Collectors.toMap(GroupProcessingCountView::getGroupId, GroupProcessingCountView::getTotal));
    }
}

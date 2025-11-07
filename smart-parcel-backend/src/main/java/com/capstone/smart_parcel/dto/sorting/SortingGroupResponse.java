package com.capstone.smart_parcel.dto.sorting;

import com.capstone.smart_parcel.domain.SortingGroup;

import java.time.OffsetDateTime;

public record SortingGroupResponse(
        Long id,
        String groupName,
        OffsetDateTime updatedAt,
        boolean enabled,
        Long managerId,
        String managerName,
        Long processingCount
) {
    public static SortingGroupResponse from(SortingGroup group) {
        return from(group, 0L);
    }

    public static SortingGroupResponse from(SortingGroup group, Long processingCount) {
        return new SortingGroupResponse(
                group.getId(),
                group.getGroupName(),
                group.getUpdatedAt(),
                group.isEnabled(),
                group.getManager() != null ? group.getManager().getId() : null,
                group.getManager() != null ? group.getManager().getName() : null,
                processingCount == null ? 0L : processingCount
        );
    }
}

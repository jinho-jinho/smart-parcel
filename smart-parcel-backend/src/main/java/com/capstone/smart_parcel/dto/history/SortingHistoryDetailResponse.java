package com.capstone.smart_parcel.dto.history;

import com.capstone.smart_parcel.domain.SortingHistory;

import java.time.OffsetDateTime;

public record SortingHistoryDetailResponse(
        Long id,
        String itemName,
        String lineName,
        OffsetDateTime processedAt,
        ImageResourceBundle images
) {

    public static SortingHistoryDetailResponse from(SortingHistory history) {
        return new SortingHistoryDetailResponse(
                history.getId(),
                history.getSortingGroupNameSnapshot(),
                history.getChuteNameSnapshot(),
                history.getProcessedAt(),
                ImageResourceBundle.single(history.getImageUrl())
        );
    }
}

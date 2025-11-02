package com.capstone.smart_parcel.dto.history;

import com.capstone.smart_parcel.domain.SortingHistory;

import java.time.OffsetDateTime;

public record SortingHistorySummaryResponse(
        Long id,
        String itemName,
        String lineName,
        OffsetDateTime processedAt
) {

    public static SortingHistorySummaryResponse from(SortingHistory history) {
        return new SortingHistorySummaryResponse(
                history.getId(),
                history.getSortingGroupNameSnapshot(),
                history.getChuteNameSnapshot(),
                history.getProcessedAt()
        );
    }
}

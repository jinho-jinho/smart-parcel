package com.capstone.smart_parcel.dto.history;

import com.capstone.smart_parcel.domain.ErrorLog;

import java.time.OffsetDateTime;

public record ErrorHistorySummaryResponse(
        Long id,
        String itemName,
        String lineName,
        String errorCode,
        OffsetDateTime occurredAt
) {

    public static ErrorHistorySummaryResponse from(ErrorLog log) {
        return new ErrorHistorySummaryResponse(
                log.getId(),
                log.getSortingGroupNameSnapshot(),
                log.getChuteNameSnapshot(),
                log.getErrorCode(),
                log.getOccurredAt()
        );
    }
}

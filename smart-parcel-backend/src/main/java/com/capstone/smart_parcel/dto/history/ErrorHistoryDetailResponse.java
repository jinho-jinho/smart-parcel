package com.capstone.smart_parcel.dto.history;

import com.capstone.smart_parcel.domain.ErrorLog;

import java.time.OffsetDateTime;

public record ErrorHistoryDetailResponse(
        Long id,
        String itemName,
        String lineName,
        String errorCode,
        OffsetDateTime occurredAt,
        ImageResourceBundle images
) {

    public static ErrorHistoryDetailResponse from(ErrorLog errorLog) {
        return new ErrorHistoryDetailResponse(
                errorLog.getId(),
                errorLog.getSortingGroupNameSnapshot(),
                errorLog.getChuteNameSnapshot(),
                errorLog.getErrorCode(),
                errorLog.getOccurredAt(),
                ImageResourceBundle.single(errorLog.getImageUrl())
        );
    }
}

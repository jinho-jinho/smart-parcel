package com.capstone.smart_parcel.dto.notification;

import com.capstone.smart_parcel.domain.UserNotification;

import java.time.OffsetDateTime;

public record NotificationResponse(
        Long id,
        boolean read,
        OffsetDateTime createdAt,
        Long errorLogId,
        String errorCode,
        OffsetDateTime occurredAt,
        String groupName,
        String chuteName
) {

    public static NotificationResponse from(UserNotification notification) {
        var errorLog = notification.getErrorLog();
        return new NotificationResponse(
                notification.getId(),
                notification.isRead(),
                notification.getCreatedAt(),
                errorLog != null ? errorLog.getId() : null,
                errorLog != null ? errorLog.getErrorCode() : null,
                errorLog != null ? errorLog.getOccurredAt() : null,
                errorLog != null ? errorLog.getSortingGroupNameSnapshot() : null,
                errorLog != null ? errorLog.getChuteNameSnapshot() : null
        );
    }
}

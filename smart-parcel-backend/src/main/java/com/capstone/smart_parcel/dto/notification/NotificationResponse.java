package com.capstone.smart_parcel.dto.notification;
import com.capstone.smart_parcel.domain.UserNotification;
import java.time.OffsetDateTime;
import java.util.UUID;

public record NotificationResponse(Long id, boolean read, OffsetDateTime createdAt, UUID eventId,
                                   long beltId, String errorCode, OffsetDateTime occurredAt) {
    public static NotificationResponse from(UserNotification n) {
        var e = n.getEvent();
        return new NotificationResponse(n.getId(), n.getReadAt() != null, n.getCreatedAt(),
                e.getEventId(), e.getBelt().getId(), e.getErrorCode(), e.getOccurredAt());
    }
}

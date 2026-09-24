package com.capstone.smart_parcel.dto.history;
import com.capstone.smart_parcel.domain.DeviceEvent;
import com.capstone.smart_parcel.domain.enums.EventType;
import java.time.OffsetDateTime;
import java.util.UUID;

public record EventResponse(UUID eventId, UUID attemptId, long beltId, EventType eventType, String errorCode,
                            OffsetDateTime occurredAt, OffsetDateTime receivedAt, String imageUri) {
    public static EventResponse from(DeviceEvent e) {
        return new EventResponse(e.getEventId(), e.getAttempt() == null ? null : e.getAttempt().getAttemptId(),
            e.getBelt().getId(), e.getEventType(), e.getErrorCode(), e.getOccurredAt(), e.getReceivedAt(), e.getImageUri());
    }
}

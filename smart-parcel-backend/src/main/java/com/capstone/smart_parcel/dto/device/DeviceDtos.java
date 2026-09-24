package com.capstone.smart_parcel.dto.device;
import com.capstone.smart_parcel.domain.enums.DecisionStatus;
import com.capstone.smart_parcel.domain.enums.EventType;
import com.capstone.smart_parcel.domain.enums.InputType;
import jakarta.validation.constraints.*;
import java.time.OffsetDateTime;
import java.util.UUID;

public final class DeviceDtos {
    private DeviceDtos() {}
    public record AppliedInput(@NotNull Long versionId) {}
    public record EventInput(@NotNull UUID eventId, UUID attemptId, @NotNull EventType eventType,
            @NotNull OffsetDateTime occurredAt, Long ruleVersionId,
            OffsetDateTime capturedAt, OffsetDateTime decidedAt, DecisionStatus decisionStatus,
            InputType recognizedInputType, @Size(max=100) String recognizedValue,
            Long ruleId, Long chuteId, Long observedChuteId, @Size(max=50) String errorCode) {}
    public record EventAck(UUID eventId, UUID attemptId, boolean duplicate) {}
}

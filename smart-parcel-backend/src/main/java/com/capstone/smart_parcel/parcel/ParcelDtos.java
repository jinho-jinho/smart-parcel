package com.capstone.smart_parcel.parcel;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

public final class ParcelDtos {
    private ParcelDtos() {}
    public record StaffInput(@NotBlank @Email @Size(max=100) String email,
                             @NotBlank @Size(max=100) String name,
                             @NotBlank @Size(min=8,max=64) String password) {}
    public record BeltInput(@NotBlank @Size(max=40) String code, @NotBlank @Size(max=100) String name) {}
    public record ChuteInput(@NotBlank @Size(max=40) String code, @NotBlank @Size(max=50) String name) {}
    public record DeviceInput(@NotBlank @Size(max=80) String code) {}
    public record ChuteSetting(@NotNull Long chuteId, @Min(0) @Max(180) int servoDeg) {}
    public record RuleInput(@NotBlank @Size(max=50) String name, @Min(1) int priority,
                            @NotNull InputType inputType, @NotBlank @Size(max=100) String inputValue,
                            @NotBlank @Size(max=100) String itemName, @NotNull Long chuteId) {}
    public record VersionInput(@NotBlank @Size(max=100) String groupName,
                               @NotEmpty @Size(max=100) List<@Valid ChuteSetting> chutes,
                               @NotEmpty @Size(max=500) List<@Valid RuleInput> rules) {}
    public record AppliedInput(@NotNull Long versionId) {}
    public enum InputType { TEXT, COLOR }
    public enum EventType { DECISION, DISCHARGE_CONFIRMED, DISCHARGE_FAILED, DEVICE_ERROR }
    public enum Decision { MATCHED, UNMATCHED, ERROR }
    public record EventInput(
            @NotNull UUID eventId, UUID attemptId, @NotNull EventType eventType,
            @NotNull OffsetDateTime occurredAt, Long ruleVersionId,
            OffsetDateTime capturedAt, OffsetDateTime decidedAt, Decision decisionStatus,
            InputType recognizedInputType, @Size(max=100) String recognizedValue,
            Long ruleId, Long chuteId, Long observedChuteId, @Size(max=50) String errorCode) {}
    public record EventAck(UUID eventId, UUID attemptId, boolean duplicate) {}
    public record DeviceCredential(UUID deviceId, long beltId, String deviceKey) {}
}

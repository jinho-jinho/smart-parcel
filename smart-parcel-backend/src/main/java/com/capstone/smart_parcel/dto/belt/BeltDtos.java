package com.capstone.smart_parcel.dto.belt;
import com.capstone.smart_parcel.domain.enums.InputType;
import com.capstone.smart_parcel.domain.enums.VersionStatus;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.time.OffsetDateTime;
import java.util.UUID;
import java.util.List;

public final class BeltDtos {
    private BeltDtos() {}
    public record IdResponse(long id) {}
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
                               @NotEmpty @Size(max=100) List<@NotNull @Valid ChuteSetting> chutes,
                               @NotEmpty @Size(max=500) List<@NotNull @Valid RuleInput> rules) {}
    public record Activation(@NotNull Long versionId, Long expectedVersionId) {}
    public record OrganizationResponse(long id, String code, String name) {}
    public record BeltResponse(long id, String code, String name, boolean enabled, Long desiredRuleVersionId) {}
    public record ChuteResponse(long id, long beltId, String code, String name, boolean enabled) {}
    public record GroupResponse(long id, long beltId, String name) {}
    public record RuleResponse(long id, String name, int priority, InputType inputType,
                               String inputValue, String itemName, long chuteId) {}
    public record VersionChuteResponse(long chuteId, String chuteName, short servoDeg) {}
    public record VersionSummary(long id, long beltId, long groupId, String groupName, int versionNo,
                                 VersionStatus status, OffsetDateTime publishedAt) {}
    public record ConfigurationResponse(long id, long beltId, long groupId, String groupName, int versionNo,
                                        VersionStatus status, List<RuleResponse> rules, List<VersionChuteResponse> chutes) {}
    public record DeviceCredential(UUID deviceId, long beltId, String deviceKey) {}
    public record DeviceResponse(UUID id, String deviceCode, boolean active, boolean credentialRevoked,
                                 Long appliedRuleVersionId, OffsetDateTime appliedAt, OffsetDateTime lastSeenAt) {}
}

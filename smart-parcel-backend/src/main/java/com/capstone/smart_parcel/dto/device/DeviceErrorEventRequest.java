package com.capstone.smart_parcel.dto.device;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.OffsetDateTime;

public record DeviceErrorEventRequest(
        @NotNull(message = "managerId is required.")
        Long managerId,
        @NotBlank(message = "errorCode is required.")
        String errorCode,
        Long ruleId,
        Long groupId,
        Long chuteId,
        @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ssXXX")
        OffsetDateTime occurredAt
) { }

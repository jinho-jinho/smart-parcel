package com.capstone.smart_parcel.dto.device;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.NotNull;

import java.time.OffsetDateTime;

public record DeviceSortingResultRequest(
        @NotNull(message = "managerId is required.")
        Long managerId,
        @NotNull(message = "ruleId is required.")
        Long ruleId,
        @NotNull(message = "chuteId is required.")
        Long chuteId,
        @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ssXXX")
        OffsetDateTime processedAt
) { }

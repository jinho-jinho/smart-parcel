package com.capstone.smart_parcel.dto.user;

import com.capstone.smart_parcel.domain.User;
import com.capstone.smart_parcel.domain.enums.Role;

import java.time.OffsetDateTime;

public record StaffSummaryResponse(
        Long id,
        String name,
        String email,
        Role role,
        OffsetDateTime createdAt
) {

    public static StaffSummaryResponse from(User user) {
        return new StaffSummaryResponse(
                user.getId(),
                user.getName(),
                user.getEmail(),
                user.getRole(),
                user.getCreatedAt()
        );
    }
}

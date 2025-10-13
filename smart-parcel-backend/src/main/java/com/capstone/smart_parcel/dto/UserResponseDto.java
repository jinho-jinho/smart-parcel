package com.capstone.smart_parcel.dto;

import com.capstone.smart_parcel.domain.enums.Role;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;

@Getter
@AllArgsConstructor
public class UserResponseDto {
    private Long id;
    private String email;
    private String name;
    private Role role;
    private OffsetDateTime createdAt;
}
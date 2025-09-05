package com.capstone.smart_parcel.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@AllArgsConstructor
public class UserResponseDto {
    private Long id;
    private String email;
    private String name;
    private LocalDateTime createdAt;
}
package com.capstone.smart_parcel.dto;
import lombok.*;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class LoginResponseDto {
    private String accessToken;
    private String tokenType;  // "Bearer"
    private long expiresIn;    // ms
}

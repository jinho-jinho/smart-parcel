package com.capstone.smart_parcel.dto;

import com.capstone.smart_parcel.domain.enums.VerificationPurpose;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class SendCodeRequest {
    @NotBlank
    @Email
    private String email;
    private VerificationPurpose purpose = VerificationPurpose.SIGNUP;
}

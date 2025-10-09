package com.capstone.smart_parcel.controller;

import com.capstone.smart_parcel.domain.enums.VerificationPurpose;
import com.capstone.smart_parcel.dto.common.ApiResponse;
import com.capstone.smart_parcel.service.EmailService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/auth")
public class EmailAuthController {

    private final EmailService emailService;

    @PostMapping("/send-code")
    public ResponseEntity<ApiResponse<String>> sendCode(
            @RequestParam String email,
            @RequestParam(defaultValue = "SIGNUP") VerificationPurpose purpose) {
        emailService.sendCode(email, purpose);
        return ResponseEntity.ok(
                new ApiResponse<>(true, null, "인증번호 전송 완료")
        );
    }

    @PostMapping("/verify-code")
    public ResponseEntity<ApiResponse<Void>> verifyCode(
            @RequestParam String email,
            @RequestParam String code,
            @RequestParam(defaultValue = "SIGNUP") VerificationPurpose purpose) {
        emailService.verifyCode(email, code, purpose);
        return ResponseEntity.ok(
                new ApiResponse<>(true, null, "이메일 인증 성공")
        );
    }
}


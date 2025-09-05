package com.capstone.smart_parcel.controller;

import com.capstone.smart_parcel.dto.ResetPasswordRequest;
import com.capstone.smart_parcel.dto.common.ApiResponse;
import com.capstone.smart_parcel.service.PasswordResetService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/user")
public class PasswordController {

    private final PasswordResetService passwordResetService;

    @PostMapping(value = "/password/reset",
            consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ApiResponse<Void>> resetPassword(@Valid @RequestBody ResetPasswordRequest req) {
        passwordResetService.resetPassword(req.getEmail(), req.getCode(), req.getNewPassword());
        return ResponseEntity.ok(new ApiResponse<>(true, null, "비밀번호가 변경되었습니다."));
    }
}

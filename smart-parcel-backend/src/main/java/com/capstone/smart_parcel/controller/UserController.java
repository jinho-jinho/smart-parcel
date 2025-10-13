package com.capstone.smart_parcel.controller;

import com.capstone.smart_parcel.dto.UserResponseDto;
import com.capstone.smart_parcel.dto.common.ApiResponse;
import com.capstone.smart_parcel.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/users")
public class UserController {

    private final UserService userService;

    /** 내 정보 */
    @GetMapping("/me")
    public ResponseEntity<ApiResponse<UserResponseDto>> me(Authentication authentication) {
        UserResponseDto userDto = userService.getByEmail(authentication.getName());
        return ResponseEntity.ok(new ApiResponse<>(true, userDto, "회원 정보 조회 성공"));
    }

}

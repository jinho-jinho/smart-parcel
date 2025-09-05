package com.capstone.smart_parcel.controller;

import com.capstone.smart_parcel.config.jwt.JwtTokenProvider;
import com.capstone.smart_parcel.config.jwt.RefreshTokenStore;
import com.capstone.smart_parcel.domain.User;
import com.capstone.smart_parcel.dto.LoginResponseDto;
import com.capstone.smart_parcel.dto.UserLoginRequestDto;
import com.capstone.smart_parcel.dto.UserResponseDto;
import com.capstone.smart_parcel.dto.UserSignupRequestDto;
import com.capstone.smart_parcel.dto.common.ApiResponse;
import com.capstone.smart_parcel.service.UserService;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.time.Duration;

@RestController
@RequiredArgsConstructor
@RequestMapping("/user")
public class UserController {

    private final UserService userService;
    private final JwtTokenProvider jwt;
    private final RefreshTokenStore rtStore;

    @Value("${jwt.access-token-validity-ms}")
    private long accessTokenValidityMs;

    @Value("${jwt.refresh-token-validity-ms}")
    private long refreshTokenValidityMs;

    /** RT 쿠키 세팅 */
    private void addRtCookie(HttpServletResponse res, String rt, long ttlMs) {
        ResponseCookie cookie = ResponseCookie.from("refresh_token", rt)
                .httpOnly(true)
                .secure(false) // SameSite=None 사용 시 true
                .path("/")
                .maxAge(Duration.ofMillis(ttlMs))
                .sameSite("Strict") // 필요에 따라 "Lax" or "None"
                .build();
        res.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
    }

    /** RT 쿠키 만료 */
    private void expireRtCookie(HttpServletResponse res) {
        ResponseCookie cookie = ResponseCookie.from("refresh_token", "")
                .httpOnly(true)
                .secure(false)
                .path("/")
                .maxAge(0)
                .sameSite("Strict")
                .build();
        res.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
    }

    @PostMapping("/signup")
    public ResponseEntity<ApiResponse<Long>> signup(@Valid @RequestBody UserSignupRequestDto dto) {
        Long userId = userService.signup(dto);
        return ResponseEntity.ok(new ApiResponse<>(true, userId, "회원가입 성공"));
    }

    /** 로그인: AT + RT 발급 (RT 쿠키 저장, jti Redis 저장) */
    @PostMapping("/login")
    public ResponseEntity<ApiResponse<LoginResponseDto>> login(@Valid @RequestBody UserLoginRequestDto dto,
                                                               HttpServletResponse response) {
        User user = userService.login(dto);

        String at = jwt.createAccessToken(user.getEmail(), user.getId());
        String rt = jwt.createRefreshToken(user.getEmail(), user.getId());

        // jti 저장
        String jti = jwt.getJti(rt);
        rtStore.saveJti(jti, user.getId(), refreshTokenValidityMs);

        // RT 쿠키 세팅
        addRtCookie(response, rt, refreshTokenValidityMs);

        var payload = new LoginResponseDto(at, "Bearer", accessTokenValidityMs);
        return ResponseEntity.ok(new ApiResponse<>(true, payload, "로그인 성공"));
    }

    /** 내 정보 */
    @GetMapping("/me")
    public ResponseEntity<ApiResponse<UserResponseDto>> me(Authentication authentication) {
        UserResponseDto userDto = userService.getByEmail(authentication.getName());
        return ResponseEntity.ok(new ApiResponse<>(true, userDto, "회원 정보 조회 성공"));
    }

    /** 토큰 재발급: 회전 (기존 jti 삭제 → 새 RT/AT 발급) */
    @PostMapping("/token/refresh")
    public ResponseEntity<ApiResponse<LoginResponseDto>> refresh(
            @CookieValue(name = "refresh_token", required = false) String rt,
            HttpServletResponse response) {

        if (rt == null || !jwt.isRefreshToken(rt)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(new ApiResponse<>(false, null, "리프레시 토큰이 유효하지 않습니다."));
        }

        String oldJti = jwt.getJti(rt);
        Long userId = jwt.getUserId(rt);
        String email = jwt.getEmail(rt);

        // Redis에 기존 jti가 존재하는지 확인 (만료/로그아웃/위조 시 실패)
        if (oldJti == null || userId == null || email == null || !rtStore.existsJti(oldJti)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(new ApiResponse<>(false, null, "리프레시 토큰 검증 실패"));
        }

        // --- 회전: 기존 jti 폐기 ---
        rtStore.deleteJti(oldJti);

        // 새 AT/RT 발급
        String newAt = jwt.createAccessToken(email, userId);
        String newRt = jwt.createRefreshToken(email, userId);
        String newJti = jwt.getJti(newRt);

        // 새 jti 저장
        rtStore.saveJti(newJti, userId, refreshTokenValidityMs);

        // RT 쿠키 교체
        addRtCookie(response, newRt, refreshTokenValidityMs);

        var payload = new LoginResponseDto(newAt, "Bearer", accessTokenValidityMs);
        return ResponseEntity.ok(new ApiResponse<>(true, payload, "토큰 재발급 성공"));
    }

    /** 로그아웃: jti 삭제 + 쿠키 만료 */
    @PostMapping("/logout")
    public ResponseEntity<ApiResponse<Void>> logout(
            @CookieValue(name = "refresh_token", required = false) String rt,
            HttpServletResponse response) {

        if (rt != null) {
            String jti = jwt.getJti(rt);
            if (jti != null) rtStore.deleteJti(jti);
        }
        expireRtCookie(response);
        return ResponseEntity.ok(new ApiResponse<>(true, null, "로그아웃 완료"));
    }
}

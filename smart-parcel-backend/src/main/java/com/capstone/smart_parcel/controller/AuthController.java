package com.capstone.smart_parcel.controller;

import com.capstone.smart_parcel.config.jwt.JwtTokenProvider;
import com.capstone.smart_parcel.config.jwt.RefreshTokenStore;
import com.capstone.smart_parcel.dto.*;
import com.capstone.smart_parcel.dto.common.ApiResponse;
import com.capstone.smart_parcel.service.EmailService;
import com.capstone.smart_parcel.service.PasswordResetService;
import com.capstone.smart_parcel.service.UserService;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Duration;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/auth")
public class AuthController {

    private final EmailService emailService;
    private final PasswordResetService passwordResetService;
    private final UserService userService;
    private final JwtTokenProvider jwt;
    private final RefreshTokenStore rtStore;

    @Value("${jwt.access-token-validity-ms}")
    private long accessTokenValidityMs;

    @Value("${jwt.refresh-token-validity-ms}")
    private long refreshTokenValidityMs;

    // 운영환경에 맞춰 설정 파일로 제어
    @Value("${security.cookie.secure:true}")         // HTTPS면 true
    private boolean cookieSecure;
    @Value("${security.cookie.samesite:None}")       // cross-site면 None
    private String cookieSameSite;
    @Value("${security.cookie.domain:}")            // 필요 시 .example.com
    private String cookieDomain;

    /** ========== 쿠키 유틸 ========== */
    private void addRtCookie(HttpServletResponse res, String rt, long ttlMs) {
        ResponseCookie.ResponseCookieBuilder b = ResponseCookie.from("refresh_token", rt)
                .httpOnly(true)
                .secure(cookieSecure)
                .path("/")
                .maxAge(Duration.ofMillis(ttlMs))
                .sameSite(cookieSameSite);
        if (cookieDomain != null && !cookieDomain.isBlank()) b.domain(cookieDomain);
        res.addHeader(HttpHeaders.SET_COOKIE, b.build().toString());
    }

    private void expireRtCookie(HttpServletResponse res) {
        ResponseCookie.ResponseCookieBuilder b = ResponseCookie.from("refresh_token", "")
                .httpOnly(true)
                .secure(cookieSecure)
                .path("/")
                .maxAge(0)
                .sameSite(cookieSameSite);
        if (cookieDomain != null && !cookieDomain.isBlank()) b.domain(cookieDomain);
        res.addHeader(HttpHeaders.SET_COOKIE, b.build().toString());
    }

    /** ========== 회원가입/로그인/토큰/로그아웃 ========== */

    @PostMapping("/signup")
    public ResponseEntity<ApiResponse<Long>> signup(@Valid @RequestBody UserSignupRequestDto dto) {
        Long userId = userService.signup(dto);
        return ResponseEntity.ok(new ApiResponse<>(true, userId, "회원가입 성공"));
    }

    /** 로그인: AT + RT 발급 (RT는 httpOnly 쿠키, RT jti 저장) */
    @PostMapping("/login")
    public ResponseEntity<ApiResponse<LoginResponseDto>> login(
            @Valid @RequestBody UserLoginRequestDto dto,
            HttpServletResponse response) {

        UserResponseDto user = userService.login(dto);

        String at = jwt.createAccessToken(user.getEmail(), user.getId());
        String rt = jwt.createRefreshToken(user.getEmail(), user.getId());

        String jti = jwt.getJti(rt);
        rtStore.saveJti(jti, user.getId(), refreshTokenValidityMs);

        addRtCookie(response, rt, refreshTokenValidityMs);

        var payload = new LoginResponseDto(at, "Bearer", accessTokenValidityMs);
        return ResponseEntity.ok(new ApiResponse<>(true, payload, "로그인 성공"));
    }

    /** 토큰 재발급(회전): 기존 jti 유효성 확인 → 삭제 → 새 AT/RT 발급/저장 */
    @PostMapping("/token/refresh")
    public ResponseEntity<ApiResponse<LoginResponseDto>> refresh(
            @CookieValue(name = "refresh_token", required = false) String rt,
            HttpServletResponse response) {

        if (rt == null || !jwt.isRefreshToken(rt)) {
            return ResponseEntity.status(401).body(new ApiResponse<>(false, null, "리프레시 토큰이 유효하지 않습니다."));
        }

        String oldJti = jwt.getJti(rt);
        Long userId   = jwt.getUserId(rt);
        String email  = jwt.getEmail(rt);

        if (oldJti == null || userId == null || email == null || !rtStore.existsJti(oldJti)) {
            return ResponseEntity.status(401).body(new ApiResponse<>(false, null, "리프레시 토큰 검증 실패"));
        }

        // 회전
        rtStore.deleteJti(oldJti);
        String newAt = jwt.createAccessToken(email, userId);
        String newRt = jwt.createRefreshToken(email, userId);
        String newJti = jwt.getJti(newRt);
        rtStore.saveJti(newJti, userId, refreshTokenValidityMs);

        addRtCookie(response, newRt, refreshTokenValidityMs);

        var payload = new LoginResponseDto(newAt, "Bearer", accessTokenValidityMs);
        return ResponseEntity.ok(new ApiResponse<>(true, payload, "토큰 재발급 성공"));
    }

    /** 로그아웃: jti 제거 + 쿠키 만료 */
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

    /** ========== 이메일 인증/비밀번호 재설정 ========== */

    @PostMapping("/send-code")
    public ResponseEntity<ApiResponse<String>> sendCode(@RequestBody @Valid SendCodeRequest req) {
        emailService.sendCode(req.getEmail(), req.getPurpose());
        return ResponseEntity.ok(new ApiResponse<>(true, null, "인증번호 전송 완료"));
    }

    @PostMapping("/verify-code")
    public ResponseEntity<ApiResponse<Void>> verifyCode(@RequestBody @Valid VerifyCodeRequest req) {
        emailService.verifyCode(req.getEmail(), req.getCode(), req.getPurpose());
        return ResponseEntity.ok(new ApiResponse<>(true, null, "이메일 인증 성공"));
    }

    @PostMapping(value = "/password/reset",
            consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ApiResponse<Void>> resetPassword(@Valid @RequestBody ResetPasswordRequest req) {
        passwordResetService.resetPassword(req.getEmail(), req.getCode(), req.getNewPassword());
        return ResponseEntity.ok(new ApiResponse<>(true, null, "비밀번호가 변경되었습니다."));
    }
}

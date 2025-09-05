package com.capstone.smart_parcel.exception;

import com.capstone.smart_parcel.dto.common.ApiResponse;
import io.jsonwebtoken.JwtException;
import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.util.NoSuchElementException;
import java.util.stream.Collectors;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    private ResponseEntity<ApiResponse<Void>> fail(HttpStatus status, String message) {
        // 4xx는 warn, 5xx는 error 레벨로 로깅
        if (status.is4xxClientError()) {
            log.warn("[{}] {}", status.value(), message);
        } else {
            log.error("[{}] {}", status.value(), message);
        }
        return ResponseEntity.status(status).body(new ApiResponse<>(false, null, message));
    }

    /** 잘못된 인자(도메인 로직) */
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ApiResponse<Void>> handleIllegalArgument(IllegalArgumentException e) {
        return fail(HttpStatus.BAD_REQUEST, e.getMessage());
    }

    /** 리소스 없음 */
    @ExceptionHandler(NoSuchElementException.class)
    public ResponseEntity<ApiResponse<Void>> handleNoSuchElement(NoSuchElementException e) {
        return fail(HttpStatus.NOT_FOUND, e.getMessage());
    }

    /** @Valid 바디 검증 실패 */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Void>> handleValidation(MethodArgumentNotValidException e) {
        String message = e.getBindingResult().getFieldErrors().stream()
                .map(err -> err.getField() + ": " + err.getDefaultMessage())
                .collect(Collectors.joining(", "));
        return fail(HttpStatus.BAD_REQUEST, message);
    }

    /** @RequestParam/@PathVariable 검증 실패 */
    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ApiResponse<Void>> handleConstraintViolation(ConstraintViolationException e) {
        String message = e.getConstraintViolations().stream()
                .map(v -> v.getPropertyPath() + ": " + v.getMessage())
                .collect(Collectors.joining(", "));
        return fail(HttpStatus.BAD_REQUEST, message);
    }

    /** 필수 파라미터 누락 */
    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<ApiResponse<Void>> handleMissingParam(MissingServletRequestParameterException e) {
        return fail(HttpStatus.BAD_REQUEST, "필수 파라미터 누락: " + e.getParameterName());
    }

    /** 타입 변환 실패 (?page=abc 같은 경우) */
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ApiResponse<Void>> handleTypeMismatch(MethodArgumentTypeMismatchException e) {
        String message = String.format("파라미터 타입이 올바르지 않습니다: %s (요청값=%s)",
                e.getName(), e.getValue());
        return fail(HttpStatus.BAD_REQUEST, message);
    }

    /** JSON 파싱 실패/본문 없음 */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiResponse<Void>> handleNotReadable(HttpMessageNotReadableException e) {
        return fail(HttpStatus.BAD_REQUEST, "요청 본문을 읽을 수 없습니다. JSON 형식/값을 확인해주세요.");
    }

    /** 지원하지 않는 메서드 */
    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ApiResponse<Void>> handleMethodNotSupported(HttpRequestMethodNotSupportedException e) {
        return fail(HttpStatus.METHOD_NOT_ALLOWED, "허용되지 않은 메서드입니다: " + e.getMethod());
    }

    /** 지원하지 않는 미디어 타입 */
    @ExceptionHandler(HttpMediaTypeNotSupportedException.class)
    public ResponseEntity<ApiResponse<Void>> handleMediaTypeNotSupported(HttpMediaTypeNotSupportedException e) {
        return fail(HttpStatus.UNSUPPORTED_MEDIA_TYPE, "지원하지 않는 Content-Type입니다.");
    }

    /** 인가 실패 (권한 없음) — 보통 AccessDeniedHandler가 처리하지만 혹시를 대비 */
    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ApiResponse<Void>> handleAccessDenied(AccessDeniedException e) {
        return fail(HttpStatus.FORBIDDEN, "접근 권한이 없습니다.");
    }

    /** JWT 파싱/검증 실패 (필터 외부에서 발생 시) */
    @ExceptionHandler(JwtException.class)
    public ResponseEntity<ApiResponse<Void>> handleJwt(JwtException e) {
        return fail(HttpStatus.UNAUTHORIZED, "토큰이 유효하지 않습니다.");
    }

    /** 그 외 모든 예외 */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleException(Exception e) {
        log.error("Unhandled exception", e);
        return fail(HttpStatus.INTERNAL_SERVER_ERROR, "알 수 없는 오류가 발생했습니다.");
    }
}

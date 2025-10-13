package com.capstone.smart_parcel.service;

import com.capstone.smart_parcel.domain.EmailVerification;
import com.capstone.smart_parcel.domain.enums.VerificationPurpose;
import com.capstone.smart_parcel.repository.EmailVerificationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.Lock;

import java.time.OffsetDateTime;

@Service
@RequiredArgsConstructor
public class PasswordResetService {

    private final UserService userService;
    private final EmailVerificationRepository repository;

    @Transactional
    public void resetPassword(String rawEmail, String code, String newPassword) {
        final String email = normalize(rawEmail);

        // (권장) 잠금으로 동시 요청 방어: 레포에 잠금 메서드 추가하는 방식
        EmailVerification v = repository.findByEmailAndPurpose(email, VerificationPurpose.RESET_PASSWORD)
                .orElseThrow(() -> new IllegalArgumentException("인증 요청을 먼저 해주세요"));

        // 만료 체크
        if (v.getExpiresAt().isBefore(OffsetDateTime.now())) {
            throw new IllegalArgumentException("인증번호가 만료되었습니다.");
        }
        // 코드 체크
        if (!v.getCode().equals(code)) {
            throw new IllegalArgumentException("인증번호가 일치하지 않습니다.");
        }

        // 비밀번호 변경 (같은 트랜잭션 참여)
        userService.changePasswordByEmail(email, newPassword);

        // 사용 완료 → 레코드 제거(재사용 방지)
        repository.deleteByEmailAndPurpose(email, VerificationPurpose.RESET_PASSWORD);
    }

    private String normalize(String email) {
        if (email == null) throw new IllegalArgumentException("이메일을 입력해 주세요.");
        String v = email.trim().toLowerCase();
        if (v.isEmpty()) throw new IllegalArgumentException("이메일을 입력해 주세요.");
        return v;
    }
}

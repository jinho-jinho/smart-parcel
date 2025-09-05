package com.capstone.smart_parcel.service;

import com.capstone.smart_parcel.domain.VerificationPurpose;
import com.capstone.smart_parcel.repository.EmailVerificationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class PasswordResetService {

    private final UserService userService;
    private final EmailVerificationRepository repository;

    @Transactional
    public void resetPassword(String email, String code, String newPassword) {
        var v = repository.findTopByEmailAndPurposeOrderByIdDesc(
                email, VerificationPurpose.RESET_PASSWORD
        ).orElseThrow(() -> new IllegalArgumentException("인증 요청을 먼저 해주세요"));

        if (v.getExpiresAt().isBefore(LocalDateTime.now()))
            throw new IllegalArgumentException("인증번호가 만료되었습니다.");
        if (!v.getCode().equals(code))
            throw new IllegalArgumentException("인증번호가 일치하지 않습니다.");

        // 비밀번호 변경 (여기 자체도 @Transactional이지만, 바깥 트랜잭션에 참여)
        userService.changePasswordByEmail(email, newPassword);

        // 같은 트랜잭션 안에서 삭제까지
        repository.deleteByEmailAndPurpose(email, VerificationPurpose.RESET_PASSWORD);
    }
}

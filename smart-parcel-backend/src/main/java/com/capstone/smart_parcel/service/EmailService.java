package com.capstone.smart_parcel.service;

import com.capstone.smart_parcel.domain.EmailVerification;
import com.capstone.smart_parcel.domain.enums.VerificationPurpose;
import com.capstone.smart_parcel.repository.EmailVerificationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.security.SecureRandom;
import java.time.OffsetDateTime;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class EmailService {

    private final JavaMailSender mailSender;
    private final EmailVerificationRepository repository;
    private static final SecureRandom RAND = new SecureRandom();

    /**
     * 인증코드 발급/재발급
     * - DB upsert는 트랜잭션 안에서
     * - 메일 전송은 커밋 이후(afterCommit) 실행
     */
    @Transactional
    public void sendCode(String rawEmail, VerificationPurpose purpose) {
        String email = normalizeAndValidate(rawEmail);
        String code = generateCode();
        OffsetDateTime expiresAt = OffsetDateTime.now().plusMinutes(5);

        // DB upsert (native upsert 사용)
        repository.upsertVerification(email, purpose.name(), code, expiresAt);

        // 메일 객체는 미리 만들어두고, 실제 전송은 커밋 이후에
        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(email);
        message.setSubject(purpose == VerificationPurpose.RESET_PASSWORD
                ? "Smart Parcel 비밀번호 재설정 인증코드"
                : "Smart Parcel 회원가입 인증코드");
        message.setText("인증번호: " + code + "\n5분 내로 입력해주세요.");

        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override public void afterCommit() {
                mailSender.send(message);
            }
        });
    }

    /**
     * 코드 검증
     * - 트랜잭션 내에서 엔티티 변경(verified=true) → JPA 더티체킹으로 반영
     */
    @Transactional
    public void verifyCode(String rawEmail, String code, VerificationPurpose purpose) {
        String email = normalizeAndValidate(rawEmail);

        EmailVerification verification = repository.findByEmailAndPurpose(email, purpose)
                .orElseThrow(() -> new IllegalArgumentException("인증 요청을 먼저 해주세요."));

        if (verification.isVerified()) {
            throw new IllegalStateException("이미 인증된 이메일입니다.");
        }
        if (verification.getExpiresAt().isBefore(OffsetDateTime.now())) {
            // 만료: 기록 삭제 후 예외
            repository.deleteByEmailAndPurpose(email, purpose);
            throw new IllegalArgumentException("인증번호가 만료되었습니다.");
        }
        if (!verification.getCode().equals(code)) {
            throw new IllegalArgumentException("인증번호가 일치하지 않습니다.");
        }

        verification.setVerified(true); // @Transactional → flush 시 반영
    }

    // ===== util =====
    private String normalizeAndValidate(String email) {
        if (email == null) throw new IllegalArgumentException("이메일을 입력해 주세요.");
        String v = email.trim().toLowerCase();
        if (v.isEmpty()) throw new IllegalArgumentException("이메일을 입력해 주세요.");
        return v;
    }

    private String generateCode() {
        int n = RAND.nextInt(1_000_000);
        return String.format("%06d", n);
    }
}

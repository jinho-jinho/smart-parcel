package com.capstone.smart_parcel.service;

import com.capstone.smart_parcel.domain.enums.VerificationPurpose;
import com.capstone.smart_parcel.repository.EmailVerificationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class EmailService {

    private final JavaMailSender mailSender;
    private final EmailVerificationRepository repository;
    private static final SecureRandom RAND = new SecureRandom();

    @Transactional
    public void sendCode(String rawEmail, VerificationPurpose purpose) {
        String email = normalize(rawEmail);
        String code = generateCode();
        LocalDateTime expiresAt = LocalDateTime.now().plusMinutes(5);

        repository.upsertVerification(email, purpose.name(), code, expiresAt);

        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(email);
        message.setSubject(purpose == VerificationPurpose.RESET_PASSWORD
                ? "Smart Parcel 비밀번호 재설정 인증코드"
                : "Smart Parcel 회원가입 인증코드");
        message.setText("인증번호: " + code + "\n5분 내로 입력해주세요.");
        mailSender.send(message);
    }

    @Transactional
    public void verifyCode(String rawEmail, String code, VerificationPurpose purpose) {
        String email = normalize(rawEmail);
        var verification = repository.findByEmailAndPurpose(email, purpose)
                .orElseThrow(() -> new IllegalArgumentException("인증 요청을 먼저 해주세요."));

        if (verification.isVerified()) throw new IllegalStateException("이미 인증된 이메일입니다.");
        if (verification.getExpiresAt().isBefore(LocalDateTime.now())) {
            repository.deleteByEmailAndPurpose(email, purpose);
            throw new IllegalArgumentException("인증번호가 만료되었습니다.");
        }
        if (!verification.getCode().equals(code)) {
            throw new IllegalArgumentException("인증번호가 일치하지 않습니다.");
        }

        verification.setVerified(true);
    }

    private String normalize(String email) {
        return email == null ? null : email.trim().toLowerCase();
    }

    private String generateCode() {
        int n = RAND.nextInt(1_000_000);
        return String.format("%06d", n);
    }
}


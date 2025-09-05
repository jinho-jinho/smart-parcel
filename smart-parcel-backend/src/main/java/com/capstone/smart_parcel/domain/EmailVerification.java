package com.capstone.smart_parcel.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(
        name = "email_verifications",
        indexes = @Index(name = "idx_email_purpose", columnList = "email, purpose"),
        uniqueConstraints = @UniqueConstraint(name = "ux_email_purpose", columnNames = {"email", "purpose"})
)
@Getter
@Setter
public class EmailVerification {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable=false, length=255)
    private String email;

    @Enumerated(EnumType.STRING)
    @Column(nullable=false, length=32)
    private VerificationPurpose purpose;

    @Column(length = 10, nullable=false)
    private String code;

    @Column(nullable=false)
    private LocalDateTime expiresAt;

    @Column(nullable=false)
    private boolean verified = false;

    public boolean isExpired() {
        return LocalDateTime.now().isAfter(expiresAt);
    }
}

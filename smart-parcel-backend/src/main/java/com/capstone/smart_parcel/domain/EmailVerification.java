package com.capstone.smart_parcel.domain;

import com.capstone.smart_parcel.domain.enums.VerificationPurpose;
import jakarta.persistence.*;
import lombok.*;

import java.time.OffsetDateTime;

@Entity
@Table(
        name = "email_verifications",
        indexes = @Index(name = "idx_email_purpose", columnList = "email, purpose"),
        uniqueConstraints = @UniqueConstraint(name = "ux_email_purpose", columnNames = {"email", "purpose"})
)
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class EmailVerification {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable=false, length=255)
    private String email;

    @Enumerated(EnumType.STRING)
    @Column(nullable=false, length=32, columnDefinition = "verification_purpose")
    private VerificationPurpose purpose;

    @Column(length = 10, nullable=false)
    private String code;

    @Column(name = "expires_at", nullable=false)
    private OffsetDateTime expiresAt;

    @Column(nullable=false)
    private boolean verified = false;

    public boolean isExpired() {
        return OffsetDateTime.now().isAfter(expiresAt);
    }
}

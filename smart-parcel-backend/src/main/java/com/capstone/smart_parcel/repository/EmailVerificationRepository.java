package com.capstone.smart_parcel.repository;

import com.capstone.smart_parcel.domain.EmailVerification;
import com.capstone.smart_parcel.domain.enums.VerificationPurpose;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.Optional;

public interface EmailVerificationRepository extends JpaRepository<EmailVerification, Long> {

    Optional<EmailVerification> findByEmailAndPurpose(String email, VerificationPurpose purpose);

    @Transactional
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query(value = """
        INSERT INTO public.email_verifications (email, purpose, code, expires_at, verified)
        VALUES (:email, CAST(:purpose AS verification_purpose), :code, :expiresAt, FALSE)
        ON CONFLICT (email, purpose)
        DO UPDATE SET
            code       = EXCLUDED.code,
            expires_at = EXCLUDED.expires_at,
            verified   = FALSE
        """, nativeQuery = true)
    void upsertVerification(@Param("email") String email,
                            @Param("purpose") String purpose,
                            @Param("code") String code,
                            @Param("expiresAt") OffsetDateTime expiresAt);

    void deleteByEmailAndPurpose(String email, VerificationPurpose purpose);
}
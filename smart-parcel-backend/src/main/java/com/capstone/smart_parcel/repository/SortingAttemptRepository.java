package com.capstone.smart_parcel.repository;
import com.capstone.smart_parcel.domain.SortingAttempt;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.repository.query.Param;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import jakarta.persistence.LockModeType;
import java.time.OffsetDateTime;
import java.util.UUID;
import java.util.Optional;

public interface SortingAttemptRepository extends JpaRepository<SortingAttempt, UUID> {

    Optional<SortingAttempt> findByOrganization_IdAndAttemptId(long org, UUID id);
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
        select a from SortingAttempt a where a.organization.id = :org and a.belt.id = :belt
        and a.device.id = :device and a.attemptId = :id
        """)
    Optional<SortingAttempt> lockByScope(@Param("org") long org, @Param("belt") long belt,
                                        @Param("device") UUID device, @Param("id") UUID id);
    Page<SortingAttempt> findByOrganization_IdAndBelt_IdAndCapturedAtGreaterThanEqualAndCapturedAtLessThan(
        long org, long belt, OffsetDateTime from, OffsetDateTime to, Pageable pageable);
    @Query("""
        select count(a) as attempts,
        coalesce(sum(case when a.decisionStatus = 'MATCHED' then 1L else 0L end),0L) as matched,
        coalesce(sum(case when a.decisionStatus = 'ERROR' then 1L else 0L end),0L) as decisionErrors,
        coalesce(sum(case when a.dischargeStatus = 'FAILED' then 1L else 0L end),0L) as dischargeFailed,
        coalesce(sum(case when a.dischargeConflict = true then 1L else 0L end),0L) as dischargeConflicts
        from SortingAttempt a where a.organization.id = :org and a.belt.id = :belt
        and a.capturedAt >= :from and a.capturedAt < :to
        """)
    Counts counts(@Param("org") long org, @Param("belt") long belt,
                  @Param("from") OffsetDateTime from, @Param("to") OffsetDateTime to);
    interface Counts {
        long getAttempts();
        long getMatched();
        long getDecisionErrors();
        long getDischargeFailed();
        long getDischargeConflicts();
    }

}

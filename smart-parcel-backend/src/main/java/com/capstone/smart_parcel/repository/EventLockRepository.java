package com.capstone.smart_parcel.repository;

import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class EventLockRepository {
    private final EntityManager entityManager;

    /** A row lock cannot protect IDs that have not been inserted yet. Held until commit/rollback. */
    public void lock(String key) {
        entityManager.createNativeQuery("select cast(pg_advisory_xact_lock(hashtextextended(?1, 0)) as text)")
                .setParameter(1, key).getSingleResult();
    }
}

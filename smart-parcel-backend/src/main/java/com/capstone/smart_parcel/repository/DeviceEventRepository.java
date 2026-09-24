package com.capstone.smart_parcel.repository;
import com.capstone.smart_parcel.domain.DeviceEvent;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import java.time.OffsetDateTime;
import java.util.UUID;
import java.util.List;
import java.util.Optional;

public interface DeviceEventRepository extends JpaRepository<DeviceEvent, UUID> {

    Optional<DeviceEvent> findByOrganization_IdAndEventId(long org, UUID id);
    List<DeviceEvent> findByOrganization_IdAndAttempt_AttemptIdOrderByOccurredAtAscEventIdAsc(long org, UUID attempt);
    Page<DeviceEvent> findByOrganization_IdAndBelt_IdAndErrorCodeIsNotNullAndOccurredAtGreaterThanEqualAndOccurredAtLessThan(
        long org, long belt, OffsetDateTime from, OffsetDateTime to, Pageable page);

}

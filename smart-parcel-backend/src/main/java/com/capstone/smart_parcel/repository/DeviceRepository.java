package com.capstone.smart_parcel.repository;
import com.capstone.smart_parcel.domain.Device;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.repository.query.Param;

import java.time.OffsetDateTime;
import java.util.UUID;
import java.util.List;
import java.util.Optional;

public interface DeviceRepository extends JpaRepository<Device, UUID> {

    Optional<Device> findByIdAndCredentialRevokedFalse(UUID id);
    Optional<Device> findByOrganization_IdAndId(long org, UUID id);
    List<Device> findByOrganization_IdAndBelt_IdOrderByCreatedAt(long org, long belt);

    @Modifying
    @Query("update Device d set d.lastSeenAt = :at where d.id = :id")
    void touch(@Param("id") UUID id, @Param("at") OffsetDateTime at);

}

package com.capstone.smart_parcel.repository;
import com.capstone.smart_parcel.domain.ConveyorBelt;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.repository.query.Param;

import jakarta.persistence.LockModeType;
import java.util.List;
import java.util.Optional;

public interface ConveyorBeltRepository extends JpaRepository<ConveyorBelt, Long> {

    List<ConveyorBelt> findByOrganization_IdOrderById(long organizationId);
    Optional<ConveyorBelt> findByOrganization_IdAndId(long organizationId, long id);
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select b from ConveyorBelt b where b.organization.id = :org and b.id = :id")
    Optional<ConveyorBelt> lockByOrganization(@Param("org") long org, @Param("id") long id);

}

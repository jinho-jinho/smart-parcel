package com.capstone.smart_parcel.repository;
import com.capstone.smart_parcel.domain.RuleVersion;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.repository.query.Param;

import jakarta.persistence.LockModeType;
import java.util.List;
import java.util.Optional;

public interface RuleVersionRepository extends JpaRepository<RuleVersion, Long> {

    Optional<RuleVersion> findByOrganization_IdAndBelt_IdAndId(long org, long belt, long id);
    List<RuleVersion> findByOrganization_IdAndBelt_IdOrderByIdDesc(long org, long belt);
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select v from RuleVersion v where v.organization.id = :org and v.belt.id = :belt and v.id = :id")
    Optional<RuleVersion> lockByScope(@Param("org") long org, @Param("belt") long belt, @Param("id") long id);
    @Query("select coalesce(max(v.versionNo), 0) from RuleVersion v where v.group.id = :group")
    int latestNumber(@Param("group") long group);

}

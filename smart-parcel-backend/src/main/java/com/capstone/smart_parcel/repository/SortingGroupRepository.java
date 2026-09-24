package com.capstone.smart_parcel.repository;
import com.capstone.smart_parcel.domain.SortingGroup;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface SortingGroupRepository extends JpaRepository<SortingGroup, Long> {

    Optional<SortingGroup> findByBelt_IdAndName(long belt, String name);
    List<SortingGroup> findByOrganization_IdAndBelt_IdOrderById(long org, long belt);

}

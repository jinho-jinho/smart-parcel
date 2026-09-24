package com.capstone.smart_parcel.repository;
import com.capstone.smart_parcel.domain.SortingRule;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface SortingRuleRepository extends JpaRepository<SortingRule, Long> {

    List<SortingRule> findByVersion_IdOrderByPriorityAscIdAsc(long version);
    Optional<SortingRule> findByOrganization_IdAndBelt_IdAndVersion_IdAndIdAndChute_Id(
        long org, long belt, long version, long id, long chute);
    long countByVersion_Id(long version);

}

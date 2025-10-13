package com.capstone.smart_parcel.repository;

import com.capstone.smart_parcel.domain.SortingRule;
import com.capstone.smart_parcel.domain.enums.InputType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SortingRuleRepository extends JpaRepository<SortingRule, Long> {

    // 그룹 내 기준 목록 / 검색
    Page<SortingRule> findByGroup_Id(Long groupId, Pageable pageable);

    Page<SortingRule> findByGroup_IdAndInputType(Long groupId, InputType inputType, Pageable pageable);

    boolean existsByGroup_IdAndRuleName(Long groupId, String ruleName);
}

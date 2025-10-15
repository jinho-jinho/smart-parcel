package com.capstone.smart_parcel.repository;

import com.capstone.smart_parcel.domain.SortingRule;
import com.capstone.smart_parcel.domain.enums.InputType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;

public interface SortingRuleRepository extends JpaRepository<SortingRule, Long> {

    // 그룹 내 기준 목록 / 검색
    Page<SortingRule> findByGroup_Id(Long groupId, Pageable pageable);

    Page<SortingRule> findByGroup_IdAndInputType(Long groupId, InputType inputType, Pageable pageable);

    boolean existsByGroup_IdAndRuleName(Long groupId, String ruleName);

    @Query("""
      SELECT r
      FROM SortingRule r
      WHERE r.group.manager.id = :managerId
        AND r.group.id = :groupId
        AND r.inputType = coalesce(:type, r.inputType)
        AND (
             coalesce(:keyword, '') = ''
          OR lower(r.ruleName)   LIKE concat('%', lower(:keyword), '%')
          OR lower(r.inputValue) LIKE concat('%', lower(:keyword), '%')
          OR lower(r.itemName)   LIKE concat('%', lower(:keyword), '%')
        )
      ORDER BY r.createdAt DESC
    """)
    Page<SortingRule> searchByGroupAndManager(@Param("managerId") Long managerId,
                                              @Param("groupId") Long groupId,
                                              @Param("type") InputType type,
                                              @Param("keyword") String keyword,
                                              Pageable pageable);

}

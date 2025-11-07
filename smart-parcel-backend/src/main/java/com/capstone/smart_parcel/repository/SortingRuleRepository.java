package com.capstone.smart_parcel.repository;

import com.capstone.smart_parcel.domain.SortingRule;
import com.capstone.smart_parcel.domain.enums.InputType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface SortingRuleRepository extends JpaRepository<SortingRule, Long> {

    List<SortingRule> findByGroup_Id(Long groupId);

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

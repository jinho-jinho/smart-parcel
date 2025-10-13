package com.capstone.smart_parcel.repository;

import com.capstone.smart_parcel.domain.RuleChute;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface RuleChuteRepository extends JpaRepository<RuleChute, Long> {

    boolean existsByRule_IdAndChute_Id(Long ruleId, Long chuteId);

    List<RuleChute> findByRule_Id(Long ruleId);

    List<RuleChute> findByChute_Id(Long chuteId);

    @Modifying
    @Query("DELETE FROM RuleChute rc WHERE rc.rule.id = :ruleId AND rc.chute.id = :chuteId")
    int deleteMapping(@Param("ruleId") Long ruleId, @Param("chuteId") Long chuteId);

    @Modifying
    @Query("DELETE FROM RuleChute rc WHERE rc.rule.id = :ruleId")
    int deleteAllByRuleId(@Param("ruleId") Long ruleId);
}

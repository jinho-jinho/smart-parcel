package com.capstone.smart_parcel.repository;

import com.capstone.smart_parcel.domain.SortingGroup;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface SortingGroupRepository extends JpaRepository<SortingGroup, Long> {

    // 분류 그룹 조회 (관리자 기준)
    Page<SortingGroup> findByManager_Id(Long managerId, Pageable pageable);
    Optional<SortingGroup> findByIdAndManager_Id(Long id, Long managerId);

    @Query("""
            SELECT g
            FROM SortingGroup g
            WHERE g.manager.id = :managerId
              AND (:enabled IS NULL OR g.enabled = :enabled)
              AND (:keyword IS NULL OR LOWER(g.groupName) LIKE :keyword)
            """)
    Page<SortingGroup> searchByManager(@Param("managerId") Long managerId,
                                       @Param("keyword") String keyword,
                                       @Param("enabled") Boolean enabled,
                                       Pageable pageable);

    // 현재 활성 그룹 조회(글로벌) 또는 관리자별 필요 시 and manager 조건 추가 버전 별도 생성 가능
    Optional<SortingGroup> findFirstByEnabledTrue();

    // 활성화/비활성화 (서비스에서 트랜잭션으로 "모두 false -> 지정 true")
    @Modifying
    @Query("UPDATE SortingGroup g SET g.enabled = FALSE")
    int disableAll();

    @Modifying
    @Query("UPDATE SortingGroup g SET g.enabled = TRUE WHERE g.id = :groupId")
    int enable(@Param("groupId") Long groupId);

    @Modifying
    @Query("UPDATE SortingGroup g SET g.enabled = FALSE WHERE g.id = :groupId")
    int disable(@Param("groupId") Long groupId);
}

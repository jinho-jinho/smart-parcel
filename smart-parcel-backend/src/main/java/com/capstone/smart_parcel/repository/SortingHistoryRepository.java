package com.capstone.smart_parcel.repository;

import com.capstone.smart_parcel.domain.SortingHistory;
import com.capstone.smart_parcel.repository.projection.DailyCountView;
import com.capstone.smart_parcel.repository.projection.LineCountView;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;

import java.time.OffsetDateTime;
import java.util.List;

public interface SortingHistoryRepository extends JpaRepository<SortingHistory, Long> {

    // 분류 이력 조회 (관리자/기간/라인/그룹 등 복합 필터 예시)
    Page<SortingHistory> findByManager_IdAndProcessedAtBetween(Long managerId, OffsetDateTime start, OffsetDateTime end, Pageable pageable);

    Page<SortingHistory> findByManager_IdAndGroup_Id(Long managerId, Long groupId, Pageable pageable);

    Page<SortingHistory> findByManager_IdAndChute_Id(Long managerId, Long chuteId, Pageable pageable);

    // 통계: 라인별
    @Query("""
           SELECT sh.chuteNameSnapshot AS chuteName, COUNT(sh) AS total
           FROM SortingHistory sh
           WHERE sh.processedAt BETWEEN :start AND :end
             AND sh.manager.id = :managerId
           GROUP BY sh.chuteNameSnapshot
           ORDER BY total DESC
           """)
    List<LineCountView> lineCountsByManagerAndDateRange(@Param("managerId") Long managerId,
                                                        @Param("start") OffsetDateTime start,
                                                        @Param("end") OffsetDateTime end);

    @Query("""
    SELECT function('date', sh.processedAt) AS day,
           COUNT(sh)                        AS total
    FROM SortingHistory sh
    WHERE sh.processedAt >= :start AND sh.processedAt < :end
      AND sh.manager.id = :managerId
    GROUP BY function('date', sh.processedAt)
    ORDER BY function('date', sh.processedAt) ASC
""")
    List<DailyCountView> dailyCountsByManagerAndDateRange(
            @Param("managerId") Long managerId,
            @Param("start") java.time.OffsetDateTime start,
            @Param("end")   java.time.OffsetDateTime end
    );


    // 오류율 계산용: 총 처리 수
    @Query("""
           SELECT COUNT(sh)
           FROM SortingHistory sh
           WHERE sh.processedAt BETWEEN :start AND :end
             AND sh.manager.id = :managerId
           """)
    Long totalProcessedByManagerAndDateRange(@Param("managerId") Long managerId,
                                             @Param("start") OffsetDateTime start,
                                             @Param("end") OffsetDateTime end);
}

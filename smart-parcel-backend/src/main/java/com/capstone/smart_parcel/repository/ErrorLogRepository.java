package com.capstone.smart_parcel.repository;

import com.capstone.smart_parcel.domain.ErrorLog;
import com.capstone.smart_parcel.repository.projection.ErrorRateView;
import com.capstone.smart_parcel.repository.projection.ErrorCodeCountView;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;

import java.time.OffsetDateTime;
import java.util.List;

public interface ErrorLogRepository extends JpaRepository<ErrorLog, Long> {

    // 오류 이력 조회 (관리자/기간/코드)
    Page<ErrorLog> findByManager_IdAndOccurredAtBetween(Long managerId, OffsetDateTime start, OffsetDateTime end, Pageable pageable);

    Page<ErrorLog> findByManager_IdAndErrorCode(Long managerId, String errorCode, Pageable pageable);

    // 오류율(Projection 하나로)
    @Query("""
  SELECT COUNT(e)
  FROM ErrorLog e
  WHERE e.occurredAt >= :start AND e.occurredAt < :end
    AND e.manager.id = :managerId
""")
    Long totalErrorsByManagerAndDateRange(@Param("managerId") Long managerId,
                                          @Param("start") java.time.OffsetDateTime start,
                                          @Param("end")   java.time.OffsetDateTime end);

    @Query("""
  SELECT COUNT(sh)
  FROM SortingHistory sh
  WHERE sh.processedAt >= :start AND sh.processedAt < :end
    AND sh.manager.id = :managerId
""")
    Long totalProcessedByManagerAndDateRange(@Param("managerId") Long managerId,
                                             @Param("start") java.time.OffsetDateTime start,
                                             @Param("end")   java.time.OffsetDateTime end);


    // 오류 코드별 통계 (막대그래프 등에 사용)
    @Query("""
           SELECT e.errorCode AS errorCode, COUNT(e) AS total
           FROM ErrorLog e
           WHERE e.occurredAt BETWEEN :start AND :end
             AND e.manager.id = :managerId
           GROUP BY e.errorCode
           ORDER BY total DESC
           """)
    List<ErrorCodeCountView> errorCountsByCodeAndDateRange(@Param("managerId") Long managerId,
                                                           @Param("start") OffsetDateTime start,
                                                           @Param("end") OffsetDateTime end);
}

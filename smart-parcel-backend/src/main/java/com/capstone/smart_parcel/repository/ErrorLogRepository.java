package com.capstone.smart_parcel.repository;

import com.capstone.smart_parcel.domain.ErrorLog;
import com.capstone.smart_parcel.repository.projection.ErrorCodeCountView;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

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

    @Query(
            value = """
                    SELECT e
                    FROM ErrorLog e
                    WHERE e.manager.id = :managerId
                      AND (:groupId IS NULL OR e.group.id = :groupId)
                      AND e.occurredAt >= COALESCE(:from, e.occurredAt)
                      AND e.occurredAt <= COALESCE(:to, e.occurredAt)
                      AND (:logId IS NULL OR e.id = :logId)
                      AND (
                          :text IS NULL
                          OR lower(e.sortingGroupNameSnapshot) LIKE :text
                          OR lower(e.chuteNameSnapshot) LIKE :text
                          OR lower(e.errorCode) LIKE :text
                      )
                    """,
            countQuery = """
                    SELECT COUNT(e)
                    FROM ErrorLog e
                    WHERE e.manager.id = :managerId
                      AND (:groupId IS NULL OR e.group.id = :groupId)
                      AND e.occurredAt >= COALESCE(:from, e.occurredAt)
                      AND e.occurredAt <= COALESCE(:to, e.occurredAt)
                      AND (:logId IS NULL OR e.id = :logId)
                      AND (
                          :text IS NULL
                          OR lower(e.sortingGroupNameSnapshot) LIKE :text
                          OR lower(e.chuteNameSnapshot) LIKE :text
                          OR lower(e.errorCode) LIKE :text
                      )
                    """
    )
    Page<ErrorLog> searchHistory(@Param("managerId") Long managerId,
                                 @Param("groupId") Long groupId,
                                 @Param("from") OffsetDateTime from,
                                 @Param("to") OffsetDateTime to,
                                 @Param("logId") Long logId,
                                 @Param("text") String text,
                                 Pageable pageable);

    Optional<ErrorLog> findByIdAndManager_Id(Long id, Long managerId);
}

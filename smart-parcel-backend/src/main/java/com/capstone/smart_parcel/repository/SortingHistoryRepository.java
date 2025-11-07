package com.capstone.smart_parcel.repository;

import com.capstone.smart_parcel.domain.SortingHistory;
import com.capstone.smart_parcel.repository.projection.DailyCountView;
import com.capstone.smart_parcel.repository.projection.GroupProcessingCountView;
import com.capstone.smart_parcel.repository.projection.LineCountView;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

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
             AND (:groupId IS NULL OR sh.group.id = :groupId)
           GROUP BY sh.chuteNameSnapshot
           ORDER BY total DESC
           """)
    List<LineCountView> lineCountsByManagerAndDateRange(@Param("managerId") Long managerId,
                                                        @Param("groupId") Long groupId,
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
            @Param("groupId") Long groupId,
            @Param("start") java.time.OffsetDateTime start,
            @Param("end")   java.time.OffsetDateTime end
    );


    // 오류율 계산용: 총 처리 수
    @Query("""
           SELECT COUNT(sh)
           FROM SortingHistory sh
           WHERE sh.processedAt BETWEEN :start AND :end
             AND sh.manager.id = :managerId
             AND (:groupId IS NULL OR sh.group.id = :groupId)
           """)
    Long totalProcessedByManagerAndDateRange(@Param("managerId") Long managerId,
                                             @Param("groupId") Long groupId,
                                             @Param("start") OffsetDateTime start,
                                             @Param("end") OffsetDateTime end);

    @Query("""
            SELECT sh.group.id AS groupId, COUNT(sh) AS total
            FROM SortingHistory sh
            WHERE sh.manager.id = :managerId
              AND sh.group.id IN :groupIds
            GROUP BY sh.group.id
            """)
    List<GroupProcessingCountView> countByGroupIds(@Param("managerId") Long managerId,
                                                   @Param("groupIds") List<Long> groupIds);

    @Query(
            value = """
                    SELECT sh
                    FROM SortingHistory sh
                    WHERE sh.manager.id = :managerId
                      AND (:groupId IS NULL OR sh.group.id = :groupId)
                      AND sh.processedAt >= COALESCE(:from, sh.processedAt)
                      AND sh.processedAt <= COALESCE(:to, sh.processedAt)
                      AND sh.id = COALESCE(:historyId, sh.id)
                      AND (
                          :text IS NULL
                          OR lower(sh.sortingGroupNameSnapshot) LIKE :text
                          OR lower(sh.chuteNameSnapshot) LIKE :text
                      )
                    """,
            countQuery = """
                    SELECT COUNT(sh)
                    FROM SortingHistory sh
                    WHERE sh.manager.id = :managerId
                      AND (:groupId IS NULL OR sh.group.id = :groupId)
                      AND sh.processedAt >= COALESCE(:from, sh.processedAt)
                      AND sh.processedAt <= COALESCE(:to, sh.processedAt)
                      AND sh.id = COALESCE(:historyId, sh.id)
                      AND (
                          :text IS NULL
                          OR lower(sh.sortingGroupNameSnapshot) LIKE :text
                          OR lower(sh.chuteNameSnapshot) LIKE :text
                      )
                    """
    )
    Page<SortingHistory> searchHistory(@Param("managerId") Long managerId,
                                       @Param("groupId") Long groupId,
                                       @Param("from") OffsetDateTime from,
                                       @Param("to") OffsetDateTime to,
                                       @Param("historyId") Long historyId,
                                       @Param("text") String text,
                                       Pageable pageable);

    Optional<SortingHistory> findByIdAndManager_Id(Long id, Long managerId);
}

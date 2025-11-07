package com.capstone.smart_parcel.service;

import com.capstone.smart_parcel.dto.stats.CountResponse;
import com.capstone.smart_parcel.dto.stats.DailyCountResponse;
import com.capstone.smart_parcel.dto.stats.ErrorRateResponse;
import com.capstone.smart_parcel.repository.ErrorLogRepository;
import com.capstone.smart_parcel.repository.SortingHistoryRepository;
import com.capstone.smart_parcel.repository.projection.DailyCountView;
import com.capstone.smart_parcel.repository.projection.LineCountView;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;

@Service
@RequiredArgsConstructor
public class StatsService {

    private final SortingContextService sortingContextService;
    private final SortingHistoryRepository sortingHistoryRepository;
    private final ErrorLogRepository errorLogRepository;

    private static final int DEFAULT_RANGE_DAYS = 7;

    @Transactional(readOnly = true)
    public List<CountResponse> countByChute(String email,
                                            OffsetDateTime from,
                                            OffsetDateTime to,
                                            Long groupId) {
        var ctx = sortingContextService.resolve(email);
        Range range = normalizeRange(from, to);
        List<LineCountView> counts = sortingHistoryRepository.lineCountsByManagerAndDateRange(
                ctx.manager().getId(),
                groupId,
                range.from(),
                range.to()
        );
        return counts.stream()
                .map(view -> CountResponse.of(view.getChuteName(), view.getTotal()))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<DailyCountResponse> dailyCounts(String email,
                                                OffsetDateTime from,
                                                OffsetDateTime to,
                                                Long groupId) {
        var ctx = sortingContextService.resolve(email);
        Range range = normalizeRange(from, to);
        List<DailyCountView> views = sortingHistoryRepository.dailyCountsByManagerAndDateRange(
                ctx.manager().getId(),
                groupId,
                range.from(),
                range.to()
        );
        return views.stream()
                .map(view -> DailyCountResponse.of(view.getDay(), view.getTotal()))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<CountResponse> countByErrorCode(String email,
                                                OffsetDateTime from,
                                                OffsetDateTime to,
                                                Long groupId) {
        var ctx = sortingContextService.resolve(email);
        Range range = normalizeRange(from, to);
        return errorLogRepository.errorCountsByCodeAndDateRange(
                ctx.manager().getId(),
                groupId,
                range.from(),
                range.to()
        ).stream().map(view -> CountResponse.of(view.getErrorCode(), view.getTotal()))
                .toList();
    }

    @Transactional(readOnly = true)
    public ErrorRateResponse errorRate(String email,
                                       OffsetDateTime from,
                                       OffsetDateTime to,
                                       Long groupId) {
        var ctx = sortingContextService.resolve(email);
        Range range = normalizeRange(from, to);

        Long totalProcessed = sortingHistoryRepository.totalProcessedByManagerAndDateRange(
                ctx.manager().getId(),
                groupId,
                range.from(),
                range.to()
        );
        Long totalErrors = errorLogRepository.totalErrorsByManagerAndDateRange(
                ctx.manager().getId(),
                groupId,
                range.from(),
                range.to()
        );
        long processed = totalProcessed == null ? 0L : totalProcessed;
        long errors = totalErrors == null ? 0L : totalErrors;
        double rate = processed == 0 ? 0.0 : (errors * 100.0) / processed;
        return ErrorRateResponse.of(processed, errors, roundTwoDecimals(rate));
    }

    private double roundTwoDecimals(double value) {
        return Math.round(value * 100.0) / 100.0;
    }

    private Range normalizeRange(OffsetDateTime from, OffsetDateTime to) {
        OffsetDateTime upper = to != null ? to : OffsetDateTime.now(ZoneOffset.UTC);
        OffsetDateTime lower = from != null ? from : upper.minusDays(DEFAULT_RANGE_DAYS);
        if (lower.isAfter(upper)) {
            throw new IllegalArgumentException("조회 시작일이 종료일보다 클 수 없습니다.");
        }
        return new Range(lower, upper);
    }

    private record Range(OffsetDateTime from, OffsetDateTime to) { }
}

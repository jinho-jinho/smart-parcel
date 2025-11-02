package com.capstone.smart_parcel.service;

import com.capstone.smart_parcel.domain.ErrorLog;
import com.capstone.smart_parcel.dto.common.PageResponse;
import com.capstone.smart_parcel.dto.history.ErrorHistoryDetailResponse;
import com.capstone.smart_parcel.dto.history.ErrorHistorySummaryResponse;
import com.capstone.smart_parcel.repository.ErrorLogRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.Locale;
import java.util.NoSuchElementException;

@Service
@RequiredArgsConstructor
public class ErrorHistoryService {

    private final ErrorLogRepository errorLogRepository;
    private final SortingContextService sortingContextService;

    @Transactional(readOnly = true)
    public PageResponse<ErrorHistorySummaryResponse> getHistory(String email,
                                                                OffsetDateTime from,
                                                                OffsetDateTime to,
                                                                String q,
                                                                Long groupId,
                                                                Pageable pageable) {
        var ctx = sortingContextService.resolve(email);
        Range range = normalizeRange(from, to);
        Keyword keyword = normalizeKeyword(q);

        Page<ErrorLog> page = errorLogRepository.searchHistory(
                ctx.manager().getId(),
                groupId,
                range.from(),
                range.to(),
                keyword.id(),
                keyword.textPattern(),
                pageable
        );

        return PageResponse.of(page, ErrorHistorySummaryResponse::from);
    }

    @Transactional(readOnly = true)
    public ErrorHistoryDetailResponse getHistoryDetail(String email, Long historyId) {
        var ctx = sortingContextService.resolve(email);
        ErrorLog errorLog = errorLogRepository.findByIdAndManager_Id(historyId, ctx.manager().getId())
                .orElseThrow(() -> new NoSuchElementException("오류 이력을 찾을 수 없습니다."));
        return ErrorHistoryDetailResponse.from(errorLog);
    }

    private Range normalizeRange(OffsetDateTime from, OffsetDateTime to) {
        if (from != null && to != null && from.isAfter(to)) {
            throw new IllegalArgumentException("검색 시작일이 종료일보다 늦습니다.");
        }
        return new Range(from, to);
    }

    private Keyword normalizeKeyword(String q) {
        if (q == null) {
            return Keyword.EMPTY;
        }
        String trimmed = q.trim();
        if (trimmed.isEmpty()) {
            return Keyword.EMPTY;
        }
        try {
            return new Keyword(Long.parseLong(trimmed), null);
        } catch (NumberFormatException ignored) {
            String pattern = "%" + trimmed.toLowerCase(Locale.ROOT) + "%";
            return new Keyword(null, pattern);
        }
    }

    private record Range(OffsetDateTime from, OffsetDateTime to) { }

    private record Keyword(Long id, String textPattern) {
        private static final Keyword EMPTY = new Keyword(null, null);
    }
}

package com.capstone.smart_parcel.controller;

import com.capstone.smart_parcel.dto.common.ApiResponse;
import com.capstone.smart_parcel.dto.common.PageResponse;
import com.capstone.smart_parcel.dto.history.SortingHistoryDetailResponse;
import com.capstone.smart_parcel.dto.history.SortingHistorySummaryResponse;
import com.capstone.smart_parcel.service.SortingHistoryService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.OffsetDateTime;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/sorting/history")
public class SortingHistoryController {

    private final SortingHistoryService sortingHistoryService;

    @GetMapping
    public ResponseEntity<ApiResponse<PageResponse<SortingHistorySummaryResponse>>> listHistory(
            Authentication authentication,
            @RequestParam(value = "from", required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime from,
            @RequestParam(value = "to", required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime to,
            @RequestParam(value = "q", required = false) String keyword,
            @RequestParam(value = "groupId", required = false) Long groupId,
            @PageableDefault(size = 20, sort = "processedAt", direction = Sort.Direction.DESC) Pageable pageable
    ) {
        PageResponse<SortingHistorySummaryResponse> response = sortingHistoryService.getHistory(
                authentication.getName(),
                from,
                to,
                keyword,
                groupId,
                pageable
        );
        return ResponseEntity.ok(new ApiResponse<>(true, response, "Fetched sorting history."));
    }

    @GetMapping("/{historyId}")
    public ResponseEntity<ApiResponse<SortingHistoryDetailResponse>> getHistory(
            Authentication authentication,
            @PathVariable Long historyId
    ) {
        SortingHistoryDetailResponse detail = sortingHistoryService.getHistoryDetail(authentication.getName(), historyId);
        return ResponseEntity.ok(new ApiResponse<>(true, detail, "Fetched sorting history detail."));
    }
}

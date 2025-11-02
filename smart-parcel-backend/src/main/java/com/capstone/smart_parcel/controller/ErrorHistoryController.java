package com.capstone.smart_parcel.controller;

import com.capstone.smart_parcel.dto.common.ApiResponse;
import com.capstone.smart_parcel.dto.common.PageResponse;
import com.capstone.smart_parcel.dto.history.ErrorHistoryDetailResponse;
import com.capstone.smart_parcel.dto.history.ErrorHistorySummaryResponse;
import com.capstone.smart_parcel.service.ErrorHistoryService;
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
@RequestMapping("/api/errors/history")
public class ErrorHistoryController {

    private final ErrorHistoryService errorHistoryService;

    @GetMapping
    public ResponseEntity<ApiResponse<PageResponse<ErrorHistorySummaryResponse>>> listHistory(
            Authentication authentication,
            @RequestParam(value = "from", required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime from,
            @RequestParam(value = "to", required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime to,
            @RequestParam(value = "q", required = false) String keyword,
            @RequestParam(value = "groupId", required = false) Long groupId,
            @PageableDefault(size = 20, sort = "occurredAt", direction = Sort.Direction.DESC) Pageable pageable
    ) {
        PageResponse<ErrorHistorySummaryResponse> response = errorHistoryService.getHistory(
                authentication.getName(),
                from,
                to,
                keyword,
                groupId,
                pageable
        );
        return ResponseEntity.ok(new ApiResponse<>(true, response, "Fetched error history."));
    }

    @GetMapping("/{historyId}")
    public ResponseEntity<ApiResponse<ErrorHistoryDetailResponse>> getHistory(
            Authentication authentication,
            @PathVariable Long historyId
    ) {
        ErrorHistoryDetailResponse detail = errorHistoryService.getHistoryDetail(authentication.getName(), historyId);
        return ResponseEntity.ok(new ApiResponse<>(true, detail, "Fetched error history detail."));
    }
}

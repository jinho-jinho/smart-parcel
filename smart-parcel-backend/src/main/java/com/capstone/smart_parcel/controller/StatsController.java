package com.capstone.smart_parcel.controller;

import com.capstone.smart_parcel.dto.common.ApiResponse;
import com.capstone.smart_parcel.dto.stats.CountResponse;
import com.capstone.smart_parcel.dto.stats.DailyCountResponse;
import com.capstone.smart_parcel.dto.stats.ErrorRateResponse;
import com.capstone.smart_parcel.service.StatsService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.OffsetDateTime;
import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/stats")
public class StatsController {

    private final StatsService statsService;

    @GetMapping("/by-chute")
    public ResponseEntity<ApiResponse<List<CountResponse>>> statsByChute(
            Authentication authentication,
            @RequestParam(value = "from", required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime from,
            @RequestParam(value = "to", required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime to,
            @RequestParam(value = "groupId", required = false) Long groupId
    ) {
        List<CountResponse> result = statsService.countByChute(authentication.getName(), from, to, groupId);
        return ResponseEntity.ok(new ApiResponse<>(true, result, "Fetched chute statistics."));
    }

    @GetMapping("/daily")
    public ResponseEntity<ApiResponse<List<DailyCountResponse>>> statsDaily(
            Authentication authentication,
            @RequestParam(value = "from", required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime from,
            @RequestParam(value = "to", required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime to,
            @RequestParam(value = "groupId", required = false) Long groupId
    ) {
        List<DailyCountResponse> result = statsService.dailyCounts(authentication.getName(), from, to, groupId);
        return ResponseEntity.ok(new ApiResponse<>(true, result, "Fetched daily statistics."));
    }

    @GetMapping("/by-error-code")
    public ResponseEntity<ApiResponse<List<CountResponse>>> statsByErrorCode(
            Authentication authentication,
            @RequestParam(value = "from", required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime from,
            @RequestParam(value = "to", required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime to,
            @RequestParam(value = "groupId", required = false) Long groupId
    ) {
        List<CountResponse> result = statsService.countByErrorCode(authentication.getName(), from, to, groupId);
        return ResponseEntity.ok(new ApiResponse<>(true, result, "Fetched error code statistics."));
    }

    @GetMapping("/error-rate")
    public ResponseEntity<ApiResponse<ErrorRateResponse>> statsErrorRate(
            Authentication authentication,
            @RequestParam(value = "from", required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime from,
            @RequestParam(value = "to", required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime to,
            @RequestParam(value = "groupId", required = false) Long groupId
    ) {
        ErrorRateResponse result = statsService.errorRate(authentication.getName(), from, to, groupId);
        return ResponseEntity.ok(new ApiResponse<>(true, result, "Fetched error rate."));
    }
}

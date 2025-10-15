package com.capstone.smart_parcel.controller;

import com.capstone.smart_parcel.dto.common.ApiResponse;
import com.capstone.smart_parcel.dto.common.PageResponse;
import com.capstone.smart_parcel.dto.sorting.ChuteCreateRequest;
import com.capstone.smart_parcel.dto.sorting.ChuteResponse;
import com.capstone.smart_parcel.dto.sorting.ChuteUpdateRequest;
import com.capstone.smart_parcel.service.ChuteService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/chutes")
public class ChuteController {

    private final ChuteService chuteService;

    @GetMapping
    public ResponseEntity<ApiResponse<PageResponse<ChuteResponse>>> listChutes(
            Authentication authentication,
            @RequestParam(value = "groupId", required = false) Long groupId,
            @RequestParam(value = "q", required = false) String keyword,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable
    ) {
        PageResponse<ChuteResponse> response = chuteService.getChutes(
                authentication.getName(),
                groupId,
                keyword,
                pageable
        );
        return ResponseEntity.ok(new ApiResponse<>(true, response, "Fetched chutes."));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<ChuteResponse>> createChute(
            Authentication authentication,
            @Valid @RequestBody ChuteCreateRequest request
    ) {
        ChuteResponse response = chuteService.createChute(authentication.getName(), request);
        return ResponseEntity.ok(new ApiResponse<>(true, response, "Created chute."));
    }

    @PatchMapping("/{chuteId}")
    public ResponseEntity<ApiResponse<ChuteResponse>> updateChute(
            Authentication authentication,
            @PathVariable Long chuteId,
            @Valid @RequestBody ChuteUpdateRequest request
    ) {
        ChuteResponse response = chuteService.updateChute(authentication.getName(), chuteId, request);
        return ResponseEntity.ok(new ApiResponse<>(true, response, "Updated chute."));
    }

    @DeleteMapping("/{chuteId}")
    public ResponseEntity<ApiResponse<Void>> deleteChute(
            Authentication authentication,
            @PathVariable Long chuteId
    ) {
        chuteService.deleteChute(authentication.getName(), chuteId);
        return ResponseEntity.ok(new ApiResponse<>(true, null, "Deleted chute."));
    }
}

package com.capstone.smart_parcel.controller;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.PostMapping;
import com.capstone.smart_parcel.dto.belt.BeltDtos.StaffInput;
import com.capstone.smart_parcel.dto.belt.BeltDtos.IdResponse;

import com.capstone.smart_parcel.dto.common.ApiResponse;
import com.capstone.smart_parcel.dto.common.PageResponse;
import com.capstone.smart_parcel.dto.user.StaffSummaryResponse;
import com.capstone.smart_parcel.service.StaffAdminService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/admin/staff")
public class AdminStaffController {

    private final StaffAdminService staffAdminService;

    @PostMapping
    public IdResponse create(
            Authentication authentication,
            @Valid @RequestBody
            StaffInput input) {
        return new IdResponse(staffAdminService.createStaff(authentication.getName(), input));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<PageResponse<StaffSummaryResponse>>> listStaff(
            Authentication authentication,
            @RequestParam(value = "q", required = false) String keyword,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable
    ) {
        PageResponse<StaffSummaryResponse> response = staffAdminService.listStaff(
                authentication.getName(),
                keyword,
                pageable
        );
        return ResponseEntity.ok(new ApiResponse<>(true, response, "Fetched staff list."));
    }

    @DeleteMapping("/{staffId}")
    public ResponseEntity<ApiResponse<Void>> deleteStaff(
            Authentication authentication,
            @PathVariable Long staffId
    ) {
        staffAdminService.deleteStaff(authentication.getName(), staffId);
        return ResponseEntity.ok(new ApiResponse<>(true, null, "Deleted staff."));
    }
}

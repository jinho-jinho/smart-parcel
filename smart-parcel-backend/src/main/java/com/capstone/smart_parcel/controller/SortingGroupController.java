package com.capstone.smart_parcel.controller;

import com.capstone.smart_parcel.domain.enums.InputType;
import com.capstone.smart_parcel.dto.common.ApiResponse;
import com.capstone.smart_parcel.dto.common.PageResponse;
import com.capstone.smart_parcel.dto.sorting.SortingGroupCreateRequest;
import com.capstone.smart_parcel.dto.sorting.SortingGroupResponse;
import com.capstone.smart_parcel.dto.sorting.SortingGroupUpdateRequest;
import com.capstone.smart_parcel.dto.sorting.SortingRuleCreateRequest;
import com.capstone.smart_parcel.dto.sorting.SortingRuleResponse;
import com.capstone.smart_parcel.service.SortingGroupService;
import com.capstone.smart_parcel.service.SortingRuleService;
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
@RequestMapping("/api/sorting-groups")
public class SortingGroupController {

    private final SortingGroupService sortingGroupService;
    private final SortingRuleService sortingRuleService;

    @GetMapping
    public ResponseEntity<ApiResponse<PageResponse<SortingGroupResponse>>> listGroups(
            Authentication authentication,
            @RequestParam(value = "q", required = false) String keyword,
            @RequestParam(value = "enabled", required = false) Boolean enabled,
            @PageableDefault(size = 20, sort = "updatedAt", direction = Sort.Direction.DESC) Pageable pageable
    ) {
        PageResponse<SortingGroupResponse> groups = sortingGroupService.getGroups(
                authentication.getName(),
                keyword,
                enabled,
                pageable
        );
        return ResponseEntity.ok(new ApiResponse<>(true, groups, "Fetched sorting groups."));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<SortingGroupResponse>> createGroup(
            Authentication authentication,
            @Valid @RequestBody SortingGroupCreateRequest request
    ) {
        SortingGroupResponse response = sortingGroupService.createGroup(authentication.getName(), request);
        return ResponseEntity.ok(new ApiResponse<>(true, response, "Created sorting group."));
    }

    @PatchMapping("/{groupId}")
    public ResponseEntity<ApiResponse<SortingGroupResponse>> updateGroup(
            Authentication authentication,
            @PathVariable Long groupId,
            @Valid @RequestBody SortingGroupUpdateRequest request
    ) {
        SortingGroupResponse response = sortingGroupService.updateGroup(authentication.getName(), groupId, request);
        return ResponseEntity.ok(new ApiResponse<>(true, response, "Updated sorting group."));
    }

    @DeleteMapping("/{groupId}")
    public ResponseEntity<ApiResponse<Void>> deleteGroup(
            Authentication authentication,
            @PathVariable Long groupId
    ) {
        sortingGroupService.deleteGroup(authentication.getName(), groupId);
        return ResponseEntity.ok(new ApiResponse<>(true, null, "Deleted sorting group."));
    }

    @PostMapping("/{groupId}/enable")
    public ResponseEntity<ApiResponse<Void>> enableGroup(
            Authentication authentication,
            @PathVariable Long groupId
    ) {
        sortingGroupService.enableGroup(authentication.getName(), groupId);
        return ResponseEntity.ok(new ApiResponse<>(true, null, "Enabled sorting group."));
    }

    @PostMapping("/{groupId}/disable")
    public ResponseEntity<ApiResponse<Void>> disableGroup(
            Authentication authentication,
            @PathVariable Long groupId
    ) {
        sortingGroupService.disableGroup(authentication.getName(), groupId);
        return ResponseEntity.ok(new ApiResponse<>(true, null, "Disabled sorting group."));
    }

    @GetMapping("/{groupId}/rules")
    public ResponseEntity<ApiResponse<PageResponse<SortingRuleResponse>>> listRules(
            Authentication authentication,
            @PathVariable Long groupId,
            @RequestParam(value = "type", required = false) InputType type,
            @RequestParam(value = "q", required = false) String keyword,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable
    ) {
        PageResponse<SortingRuleResponse> response = sortingRuleService.getRules(
                authentication.getName(),
                groupId,
                type,
                keyword,
                pageable
        );
        return ResponseEntity.ok(new ApiResponse<>(true, response, "Fetched sorting rules."));
    }

    @PostMapping("/{groupId}/rules")
    public ResponseEntity<ApiResponse<SortingRuleResponse>> createRule(
            Authentication authentication,
            @PathVariable Long groupId,
            @Valid @RequestBody SortingRuleCreateRequest request
    ) {
        SortingRuleResponse response = sortingRuleService.createRule(authentication.getName(), groupId, request);
        return ResponseEntity.ok(new ApiResponse<>(true, response, "Created sorting rule."));
    }
}

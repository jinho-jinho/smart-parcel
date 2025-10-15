package com.capstone.smart_parcel.controller;

import com.capstone.smart_parcel.dto.common.ApiResponse;
import com.capstone.smart_parcel.dto.sorting.SortingRuleResponse;
import com.capstone.smart_parcel.dto.sorting.SortingRuleUpdateRequest;
import com.capstone.smart_parcel.service.SortingRuleService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/sorting-rules")
public class SortingRuleController {

    private final SortingRuleService sortingRuleService;

    @PatchMapping("/{ruleId}")
    public ResponseEntity<ApiResponse<SortingRuleResponse>> updateRule(
            Authentication authentication,
            @PathVariable Long ruleId,
            @Valid @RequestBody SortingRuleUpdateRequest request
    ) {
        SortingRuleResponse response = sortingRuleService.updateRule(
                authentication.getName(),
                ruleId,
                request
        );
        return ResponseEntity.ok(new ApiResponse<>(true, response, "Updated sorting rule."));
    }

    @DeleteMapping("/{ruleId}")
    public ResponseEntity<ApiResponse<Void>> deleteRule(
            Authentication authentication,
            @PathVariable Long ruleId
    ) {
        sortingRuleService.deleteRule(authentication.getName(), ruleId);
        return ResponseEntity.ok(new ApiResponse<>(true, null, "Deleted sorting rule."));
    }
}

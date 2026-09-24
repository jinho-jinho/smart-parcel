package com.capstone.smart_parcel.controller;
import com.capstone.smart_parcel.service.SortingRuleService;
import com.capstone.smart_parcel.dto.belt.BeltDtos.*;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequiredArgsConstructor

@RequestMapping("/api/belts/{belt}/versions")
public class SortingRuleController {
    private final SortingRuleService rules;
    @GetMapping
    public List<VersionSummary> list(Authentication a, @PathVariable long belt) { return rules.list(a.getName(), belt); }
    @PostMapping
    public IdResponse create(Authentication a, @PathVariable long belt, @Valid @RequestBody VersionInput in) {
        return new IdResponse(rules.createVersion(a.getName(), belt, in));
    }
    @GetMapping("/{version}")
    public ConfigurationResponse get(Authentication a, @PathVariable long belt, @PathVariable long version) {
        return rules.get(a.getName(), belt, version);
    }
    @PostMapping("/{version}/publish")
    public ConfigurationResponse publish(Authentication a, @PathVariable long belt, @PathVariable long version) {
        return rules.publish(a.getName(), belt, version);
    }
}

package com.capstone.smart_parcel.controller;
import com.capstone.smart_parcel.service.SortingGroupService;
import com.capstone.smart_parcel.dto.belt.BeltDtos.*;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequiredArgsConstructor

@RequestMapping("/api/belts/{belt}/sorting-groups")
public class SortingGroupController {
    private final SortingGroupService groups;
    @GetMapping
    public List<GroupResponse> list(Authentication a, @PathVariable long belt) { return groups.list(a.getName(), belt); }
}

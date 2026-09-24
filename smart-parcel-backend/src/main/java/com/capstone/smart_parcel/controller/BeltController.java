package com.capstone.smart_parcel.controller;
import com.capstone.smart_parcel.service.BeltService;
import com.capstone.smart_parcel.dto.belt.BeltDtos.*;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequiredArgsConstructor

@RequestMapping("/api")
public class BeltController {
    private final BeltService belts;
    @GetMapping("/organization")
    public OrganizationResponse organization(Authentication a) { return belts.organization(a.getName()); }
    @GetMapping("/belts")
    public List<BeltResponse> list(Authentication a) { return belts.list(a.getName()); }
    @PostMapping("/belts")
    public IdResponse create(Authentication a, @Valid @RequestBody BeltInput in) {
        return new IdResponse(belts.createBelt(a.getName(), in));
    }
    @PutMapping("/belts/{belt}/active-version")
    public void activate(Authentication a, @PathVariable long belt, @Valid @RequestBody Activation in) {
        belts.activate(a.getName(), belt, in.versionId(), in.expectedVersionId());
    }
}

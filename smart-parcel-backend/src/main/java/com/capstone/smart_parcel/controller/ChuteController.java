package com.capstone.smart_parcel.controller;
import com.capstone.smart_parcel.service.ChuteService;
import com.capstone.smart_parcel.dto.belt.BeltDtos.*;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequiredArgsConstructor

@RequestMapping("/api/belts/{belt}/chutes")
public class ChuteController {
    private final ChuteService chutes;
    @GetMapping
    public List<ChuteResponse> list(Authentication a, @PathVariable long belt) { return chutes.list(a.getName(), belt); }
    @PostMapping
    public IdResponse create(Authentication a, @PathVariable long belt, @Valid @RequestBody ChuteInput in) {
        return new IdResponse(chutes.createChute(a.getName(), belt, in));
    }
}

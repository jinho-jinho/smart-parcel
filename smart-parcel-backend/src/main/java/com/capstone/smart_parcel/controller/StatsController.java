package com.capstone.smart_parcel.controller;
import java.time.OffsetDateTime;
import com.capstone.smart_parcel.dto.stats.AttemptStatsResponse;
import com.capstone.smart_parcel.service.StatsService;
import com.capstone.smart_parcel.dto.belt.BeltDtos.*;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor

@RequestMapping("/api/stats")
public class StatsController {
    private final StatsService stats;
    @GetMapping
    public AttemptStatsResponse stats(Authentication a, @RequestParam long beltId,
            @RequestParam OffsetDateTime from, @RequestParam OffsetDateTime to) {
        return stats.stats(a.getName(), beltId, from, to);
    }
}

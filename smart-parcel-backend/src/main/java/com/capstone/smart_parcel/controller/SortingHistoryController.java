package com.capstone.smart_parcel.controller;
import java.time.OffsetDateTime;
import com.capstone.smart_parcel.dto.history.EventResponse;
import com.capstone.smart_parcel.dto.history.AttemptResponse;
import com.capstone.smart_parcel.dto.common.PageResponse;
import com.capstone.smart_parcel.service.SortingHistoryService;
import com.capstone.smart_parcel.dto.belt.BeltDtos.*;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import java.util.UUID;
import java.util.List;

@RestController
@RequiredArgsConstructor

@RequestMapping({"/api/attempts", "/api/sorting/history"})
public class SortingHistoryController {
    private final SortingHistoryService history;
    @GetMapping
    public PageResponse<AttemptResponse> list(
            Authentication a, @RequestParam long beltId, @RequestParam OffsetDateTime from,
            @RequestParam OffsetDateTime to, @RequestParam(defaultValue="0") int page,
            @RequestParam(defaultValue="50") int size) {
        return history.history(a.getName(), beltId, from, to, page, size);
    }
    @GetMapping("/{id}")
    public AttemptResponse detail(Authentication a, @PathVariable UUID id) {
        return history.detail(a.getName(), id);
    }
    @GetMapping("/{id}/events")
    public List<EventResponse> events(Authentication a, @PathVariable UUID id) {
        return history.events(a.getName(), id);
    }
}

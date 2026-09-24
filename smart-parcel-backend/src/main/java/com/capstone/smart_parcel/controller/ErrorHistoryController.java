package com.capstone.smart_parcel.controller;
import java.time.OffsetDateTime;
import com.capstone.smart_parcel.dto.history.EventResponse;
import com.capstone.smart_parcel.dto.common.PageResponse;
import com.capstone.smart_parcel.service.SortingHistoryService;
import com.capstone.smart_parcel.dto.belt.BeltDtos.*;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor

@RequestMapping("/api/errors/history")
public class ErrorHistoryController {
    private final SortingHistoryService history;
    @GetMapping
    public PageResponse<EventResponse> list(
            Authentication a, @RequestParam long beltId, @RequestParam OffsetDateTime from,
            @RequestParam OffsetDateTime to, @RequestParam(defaultValue="0") int page,
            @RequestParam(defaultValue="50") int size) {
        return history.errors(a.getName(), beltId, from, to, page, size);
    }
}

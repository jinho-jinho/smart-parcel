package com.capstone.smart_parcel.controller;
import com.capstone.smart_parcel.service.SortingHistoryService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.*;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/events")
public class EventImageController {
    private final SortingHistoryService history;
    @GetMapping("/{id}/image")
    public ResponseEntity<byte[]> image(Authentication a, @PathVariable UUID id) {
        var image = history.image(a.getName(), id);
        return ResponseEntity.ok().cacheControl(CacheControl.noStore()).header("X-Content-Type-Options", "nosniff")
                .contentType(MediaType.parseMediaType(image.contentType())).body(image.bytes());
    }
}

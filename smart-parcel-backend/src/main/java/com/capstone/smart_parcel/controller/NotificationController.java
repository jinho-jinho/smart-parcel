package com.capstone.smart_parcel.controller;

import com.capstone.smart_parcel.dto.common.ApiResponse;
import com.capstone.smart_parcel.dto.common.PageResponse;
import com.capstone.smart_parcel.config.jwt.JwtTokenProvider;
import com.capstone.smart_parcel.dto.notification.NotificationResponse;
import com.capstone.smart_parcel.service.NotificationService;
import com.capstone.smart_parcel.service.NotificationStreamService;
import com.capstone.smart_parcel.service.SortingContextService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/notifications")
public class NotificationController {

    private final NotificationService notificationService;
    private final NotificationStreamService notificationStreamService;
    private final SortingContextService sortingContextService;
    private final JwtTokenProvider jwtTokenProvider;

    @GetMapping
    public ResponseEntity<ApiResponse<PageResponse<NotificationResponse>>> listNotifications(
            Authentication authentication,
            @RequestParam(value = "unreadOnly", required = false) Boolean unreadOnly,
            @PageableDefault(size = 10, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable
    ) {
        PageResponse<NotificationResponse> response = notificationService.getNotifications(
                authentication.getName(),
                unreadOnly,
                pageable
        );
        return ResponseEntity.ok(new ApiResponse<>(true, response, "Fetched notifications."));
    }

    @GetMapping("/stream")
    public SseEmitter stream(Authentication authentication,
                             @RequestParam(value = "token", required = false) String token) {
        String email = null;
        if (authentication != null && authentication.isAuthenticated()) {
            email = authentication.getName();
        } else if (token != null && !token.isBlank()) {
            if (!jwtTokenProvider.isAccessToken(token)) {
                throw new IllegalArgumentException("Invalid token supplied.");
            }
            email = jwtTokenProvider.getEmail(token);
        }
        if (email == null || email.isBlank()) {
            throw new IllegalArgumentException("Authentication required for notifications.");
        }
        var ctx = sortingContextService.resolve(email);
        return notificationStreamService.subscribe(ctx.actor().getId());
    }

    @PatchMapping("/{notificationId}/read")
    public ResponseEntity<ApiResponse<Void>> markAsRead(
            Authentication authentication,
            @PathVariable Long notificationId
    ) {
        notificationService.markAsRead(authentication.getName(), notificationId);
        return ResponseEntity.ok(new ApiResponse<>(true, null, "Notification marked as read."));
    }
}

package com.capstone.smart_parcel.service;
import com.capstone.smart_parcel.domain.DeviceEvent;
import com.capstone.smart_parcel.domain.UserNotification;
import com.capstone.smart_parcel.dto.common.PageResponse;
import com.capstone.smart_parcel.dto.notification.NotificationResponse;
import com.capstone.smart_parcel.repository.UserNotificationRepository;
import com.capstone.smart_parcel.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionalEventListener;
import java.time.OffsetDateTime;

@Service
@RequiredArgsConstructor
public class NotificationService {
    private final SortingContextService access;
    private final UserRepository users;
    private final UserNotificationRepository notifications;
    private final ApplicationEventPublisher publisher;
    private final NotificationStreamService streams;
    private record Created(long userId, NotificationResponse notification) {}

    @Transactional
    public void record(DeviceEvent event) {
        for (var user : users.findByOrganization_Id(event.getOrganization().getId())) {
            var n = notifications.save(UserNotification.builder().organization(event.getOrganization())
                    .event(event).recipient(user).build());
            publisher.publishEvent(new Created(user.getId(), NotificationResponse.from(n)));
        }
    }
    // No notification can escape a rolled-back event transaction.
    @TransactionalEventListener
    public void deliver(Created created) { streams.sendNotification(created.userId(), created.notification()); }

    @Transactional(readOnly = true)
    public PageResponse<NotificationResponse> getNotifications(String email, Boolean unreadOnly, Pageable requested) {
        var user = access.actor(email, false);
        var page = SortingHistoryService.page(requested.getPageNumber(), requested.getPageSize(), "createdAt", "id");
        var result = Boolean.TRUE.equals(unreadOnly)
                ? notifications.findByOrganization_IdAndRecipient_IdAndReadAtIsNull(user.getOrganizationId(), user.getId(), page)
                : notifications.findByOrganization_IdAndRecipient_Id(user.getOrganizationId(), user.getId(), page);
        return PageResponse.of(result, NotificationResponse::from);
    }
    @Transactional
    public void markAsRead(String email, Long id) {
        var user = access.actor(email, false);
        var n = notifications.findByOrganization_IdAndRecipient_IdAndId(user.getOrganizationId(), user.getId(), id)
                .orElseThrow(SortingContextService::notFound);
        if (n.getReadAt() == null) n.setReadAt(OffsetDateTime.now());
    }
}

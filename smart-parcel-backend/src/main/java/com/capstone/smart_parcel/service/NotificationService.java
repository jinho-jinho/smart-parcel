package com.capstone.smart_parcel.service;

import com.capstone.smart_parcel.domain.ErrorLog;
import com.capstone.smart_parcel.domain.User;
import com.capstone.smart_parcel.domain.UserNotification;
import com.capstone.smart_parcel.domain.enums.Role;
import com.capstone.smart_parcel.dto.common.PageResponse;
import com.capstone.smart_parcel.dto.notification.NotificationResponse;
import com.capstone.smart_parcel.repository.UserNotificationRepository;
import com.capstone.smart_parcel.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;

@Service
@RequiredArgsConstructor
public class NotificationService {

    private final SortingContextService sortingContextService;
    private final UserNotificationRepository userNotificationRepository;
    private final UserRepository userRepository;

    @Transactional
    public void notifyError(ErrorLog errorLog) {
        if (errorLog == null || errorLog.getManager() == null) {
            return;
        }
        User manager = errorLog.getManager();
        Map<Long, User> recipients = new LinkedHashMap<>();
        recipients.put(manager.getId(), manager);
        List<User> staff = userRepository.findByManager_IdAndRole(manager.getId(), Role.STAFF);
        for (User staffMember : staff) {
            if (staffMember != null) {
                recipients.put(staffMember.getId(), staffMember);
            }
        }
        if (recipients.isEmpty()) {
            return;
        }
        List<UserNotification> notifications = recipients.values().stream()
                .map(user -> UserNotification.builder()
                        .recipient(user)
                        .errorLog(errorLog)
                        .build())
                .toList();
        userNotificationRepository.saveAll(notifications);
    }

    @Transactional(readOnly = true)
    public PageResponse<NotificationResponse> getNotifications(String email, Boolean unreadOnly, Pageable pageable) {
        var ctx = sortingContextService.resolve(email);
        User actor = ctx.actor();
        Page<UserNotification> page;
        if (Boolean.TRUE.equals(unreadOnly)) {
            page = userNotificationRepository.findByRecipient_IdAndIsReadFalse(actor.getId(), pageable);
        } else {
            page = userNotificationRepository.findByRecipient_Id(actor.getId(), pageable);
        }
        return PageResponse.of(page, NotificationResponse::from);
    }

    @Transactional
    public void markAsRead(String email, Long notificationId) {
        var ctx = sortingContextService.resolve(email);
        User actor = ctx.actor();
        UserNotification notification = userNotificationRepository.findByIdAndRecipient_Id(notificationId, actor.getId())
                .orElseThrow(() -> new NoSuchElementException("알림을 찾을 수 없습니다."));
        notification.setRead(true);
    }
}

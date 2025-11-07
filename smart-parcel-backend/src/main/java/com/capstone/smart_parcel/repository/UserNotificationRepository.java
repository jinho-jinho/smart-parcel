package com.capstone.smart_parcel.repository;

import com.capstone.smart_parcel.domain.UserNotification;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserNotificationRepository extends JpaRepository<UserNotification, Long> {

    Page<UserNotification> findByRecipient_Id(Long recipientId, Pageable pageable);

    Page<UserNotification> findByRecipient_IdAndIsReadFalse(Long recipientId, Pageable pageable);

    Optional<UserNotification> findByIdAndRecipient_Id(Long id, Long recipientId);
}

package com.capstone.smart_parcel.repository;
import com.capstone.smart_parcel.domain.UserNotification;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import java.util.Optional;

public interface UserNotificationRepository extends JpaRepository<UserNotification, Long> {

    @EntityGraph(attributePaths = {"event"})
    Page<UserNotification> findByOrganization_IdAndRecipient_Id(long org, long user, Pageable page);
    @EntityGraph(attributePaths = {"event"})
    Page<UserNotification> findByOrganization_IdAndRecipient_IdAndReadAtIsNull(long org, long user, Pageable page);
    Optional<UserNotification> findByOrganization_IdAndRecipient_IdAndId(long org, long user, long id);
    void deleteByRecipient_Id(long user);

}

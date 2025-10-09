// com/capstone/smart_parcel/domain/UserNotification.java
package com.capstone.smart_parcel.domain;

import jakarta.persistence.*;
import lombok.*;

import java.time.OffsetDateTime;

@Entity
@Table(
        name = "user_notifications",
        indexes = {
                @Index(name = "idx_un_read_recipient", columnList = "recipient_user_id, is_read")
        }
)
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class UserNotification {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name="created_at", nullable = false)
    private OffsetDateTime createdAt = OffsetDateTime.now();

    @Column(name="is_read", nullable = false)
    private boolean isRead = false;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "error_log_id",
            foreignKey = @ForeignKey(name = "fk_un_error"))
    private ErrorLog errorLog;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "recipient_user_id",
            foreignKey = @ForeignKey(name = "fk_un_recipient"))
    private User recipient;
}

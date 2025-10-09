// com/capstone/smart_parcel/domain/ErrorLog.java
package com.capstone.smart_parcel.domain;

import jakarta.persistence.*;
import lombok.*;

import java.time.OffsetDateTime;

@Entity
@Table(
        name = "error_logs",
        indexes = {
                @Index(name = "idx_error_code", columnList = "error_code"),
                @Index(name = "idx_error_occurred", columnList = "occurred_at"),
                @Index(name = "idx_error_manager_id", columnList = "manager_id")
        }
)
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class ErrorLog {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name="error_code", nullable = false, length = 50)
    private String errorCode;

    @Column(name="occurred_at", nullable = false)
    private OffsetDateTime occurredAt = OffsetDateTime.now();

    @Column(name="image_url", length = 512)
    private String imageUrl;

    @Column(name="sorting_group_name_snapshot", length = 50)
    private String sortingGroupNameSnapshot;

    @Column(name="chute_name_snapshot", length = 50)
    private String chuteNameSnapshot;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "manager_id",
            foreignKey = @ForeignKey(name = "fk_err_manager"))
    private User manager;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "group_id",
            foreignKey = @ForeignKey(name = "fk_err_group"))
    private SortingGroup group;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "chute_id",
            foreignKey = @ForeignKey(name = "fk_err_chute"))
    private Chute chute;
}

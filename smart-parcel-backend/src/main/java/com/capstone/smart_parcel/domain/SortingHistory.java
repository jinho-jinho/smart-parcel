// com/capstone/smart_parcel/domain/SortingHistory.java
package com.capstone.smart_parcel.domain;

import jakarta.persistence.*;
import lombok.*;

import java.time.OffsetDateTime;

@Entity
@Table(
        name = "sorting_history",
        indexes = {
                @Index(name = "idx_hist_processed_at", columnList = "processed_at"),
                @Index(name = "idx_hist_chute_id", columnList = "chute_id"),
                @Index(name = "idx_hist_manager_id", columnList = "manager_id")
        }
)
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class SortingHistory {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "image_url", nullable = false, length = 512)
    private String imageUrl;

    @Column(name = "processed_at", nullable = false)
    private OffsetDateTime processedAt = OffsetDateTime.now();

    @Column(name = "sorting_group_name_snapshot", nullable = false, length = 50)
    private String sortingGroupNameSnapshot;

    @Column(name = "chute_name_snapshot", nullable = false, length = 50)
    private String chuteNameSnapshot;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "manager_id",
            foreignKey = @ForeignKey(name = "fk_hist_manager"))
    private User manager;

    @ManyToOne(fetch = FetchType.LAZY, optional = true)
    @JoinColumn(name = "group_id",
            foreignKey = @ForeignKey(name = "fk_hist_group"))
    private SortingGroup group;

    @ManyToOne(fetch = FetchType.LAZY, optional = true)
    @JoinColumn(name = "chute_id",
            foreignKey = @ForeignKey(name = "fk_hist_chute"))
    private Chute chute;
}

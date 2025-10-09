package com.capstone.smart_parcel.domain;

import jakarta.persistence.*;
import lombok.*;

import java.time.OffsetDateTime;

@Entity
@Table(
        name = "sorting_groups",
        indexes = {
                @Index(name = "idx_groups_manager_id", columnList = "manager_id")
        }
        // 활성 그룹 1개(부분 유니크)는 DB 인덱스로 강제
)
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class SortingGroup {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name="group_name", nullable = false, length = 50)
    private String groupName;

    @Column(nullable = false)
    private OffsetDateTime updatedAt = OffsetDateTime.now();

    @Column(nullable = false)
    private boolean enabled = false;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "manager_id",
            foreignKey = @ForeignKey(name = "fk_groups_manager"))
    private User manager;
}

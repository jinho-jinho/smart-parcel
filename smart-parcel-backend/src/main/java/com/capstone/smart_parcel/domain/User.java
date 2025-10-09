
package com.capstone.smart_parcel.domain;

import com.capstone.smart_parcel.domain.enums.Role;
import jakarta.persistence.*;
import lombok.*;

import java.time.OffsetDateTime;

@Entity
@Table(
        name = "users",
        uniqueConstraints = @UniqueConstraint(name = "uq_users_email", columnNames = "email"),
        indexes = { @Index(name = "idx_users_manager_id", columnList = "manager_id") }
)
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class User {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String email;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(nullable = false, length = 255)
    private String password;

    @Column(length = 20)
    private String bizNumber; // 옵션

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, columnDefinition = "user_role")
    private Role role = Role.STAFF;

    @Column(nullable = false)
    private OffsetDateTime createdAt = OffsetDateTime.now();

    // self reference (optional)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "manager_id",
            foreignKey = @ForeignKey(name = "fk_users_manager"))
    private User manager;
}

package com.capstone.smart_parcel.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Getter
@Setter
@Table(name = "users")
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false, length = 100)
    private String email;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(nullable = false)
    private String password;

    @Column(unique = true, length = 20)
    private String bizNumber; // 사업자 등록번호(선택)

    @Enumerated(EnumType.STRING)   // "ADMIN", "STAFF"
    @Column(nullable = false, length = 20)
    private UserRole role;

    @CreationTimestamp
    private LocalDateTime createdAt;

}

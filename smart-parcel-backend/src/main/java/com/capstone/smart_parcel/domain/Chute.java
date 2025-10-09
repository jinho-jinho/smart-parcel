// com/capstone/smart_parcel/domain/Chute.java
package com.capstone.smart_parcel.domain;

import jakarta.persistence.*;
import lombok.*;

import java.time.OffsetDateTime;

@Entity
@Table(
        name = "chutes",
        uniqueConstraints = @UniqueConstraint(
                name = "uq_chute",
                columnNames = {"servo_deg", "chute_name"}
        )
)
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Chute {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name="chute_name", nullable = false, length = 50)
    private String chuteName;

    @Column(name="servo_deg", nullable = false)
    private Short servoDeg;

    @Column(nullable = false)
    private OffsetDateTime createdAt = OffsetDateTime.now();
}

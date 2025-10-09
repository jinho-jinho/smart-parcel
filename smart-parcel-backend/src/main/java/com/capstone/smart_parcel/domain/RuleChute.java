// com/capstone/smart_parcel/domain/RuleChute.java
package com.capstone.smart_parcel.domain;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(
        name = "rule_chutes",
        uniqueConstraints = @UniqueConstraint(
                name = "uq_rule_chute",
                columnNames = {"rule_id", "chute_id"}
        ),
        indexes = {
                @Index(name = "idx_rc_rule_id", columnList = "rule_id"),
                @Index(name = "idx_rc_chute_id", columnList = "chute_id")
        }
)
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class RuleChute {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "rule_id",
            foreignKey = @ForeignKey(name = "fk_rc_rule"))
    private SortingRule rule;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "chute_id",
            foreignKey = @ForeignKey(name = "fk_rc_chute"))
    private Chute chute;
}

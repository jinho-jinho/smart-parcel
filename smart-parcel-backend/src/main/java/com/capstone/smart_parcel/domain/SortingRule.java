// com/capstone/smart_parcel/domain/SortingRule.java
package com.capstone.smart_parcel.domain;

import com.capstone.smart_parcel.domain.enums.InputType;
import jakarta.persistence.*;
import lombok.*;

import java.time.OffsetDateTime;

@Entity
@Table(
        name = "sorting_rules",
        indexes = {
                @Index(name = "idx_rules_group_id", columnList = "group_id"),
                @Index(name = "idx_rules_input_type_value", columnList = "input_type, input_value")
        }
)
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class SortingRule {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name="rule_name", nullable = false, length = 50)
    private String ruleName;

    @Enumerated(EnumType.STRING)
    @Column(name="input_type", nullable = false, columnDefinition = "input_type")
    private InputType inputType;

    @Column(name="input_value", nullable = false, length = 50)
    private String inputValue;

    @Column(nullable = false)
    private OffsetDateTime createdAt = OffsetDateTime.now();

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "group_id",
            foreignKey = @ForeignKey(name = "fk_rules_group"))
    private SortingGroup group;

    @Column(name="item_name", nullable = false, length = 100)
    private String itemName;
}

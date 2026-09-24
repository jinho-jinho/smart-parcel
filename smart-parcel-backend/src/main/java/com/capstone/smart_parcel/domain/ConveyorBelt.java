package com.capstone.smart_parcel.domain;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.persistence.Id;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Column;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.FetchType;
import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;
import java.time.OffsetDateTime;

@Entity
@Table(name = "conveyor_belts")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class ConveyorBelt {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "organization_id", nullable = false)
    private Organization organization;
    @Column(nullable = false, length = 40)
    private String code;
    @Column(nullable = false, length = 100)
    private String name;
    @Builder.Default
    private boolean enabled = true;
    @ManyToOne(fetch = FetchType.LAZY, optional = true)
    @JoinColumn(name = "desired_rule_version_id", nullable = true)
    private RuleVersion desiredRuleVersion;

    private OffsetDateTime desiredSetAt;
    @Builder.Default @Column(nullable = false, updatable = false)
    private OffsetDateTime createdAt = OffsetDateTime.now();

}

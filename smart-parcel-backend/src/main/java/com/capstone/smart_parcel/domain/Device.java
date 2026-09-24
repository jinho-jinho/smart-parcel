package com.capstone.smart_parcel.domain;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.persistence.Id;
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
import java.util.UUID;

@Entity
@org.hibernate.annotations.DynamicUpdate
@Table(name = "devices")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Device {
    @Id
    private UUID id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "organization_id", nullable = false)
    private Organization organization;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "belt_id", nullable = false)
    private ConveyorBelt belt;

    private String deviceCode;

    private String credentialHash;
    @Builder.Default
    private boolean active = true;

    private boolean credentialRevoked;
    @ManyToOne(fetch = FetchType.LAZY, optional = true)
    @JoinColumn(name = "applied_rule_version_id", nullable = true)
    private RuleVersion appliedRuleVersion;

    private OffsetDateTime appliedAt;

    private OffsetDateTime lastSeenAt;
    @Builder.Default @Column(nullable = false, updatable = false)
    private OffsetDateTime createdAt = OffsetDateTime.now();

}

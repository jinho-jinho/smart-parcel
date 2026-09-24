package com.capstone.smart_parcel.domain;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.persistence.Id;
import jakarta.persistence.Column;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.FetchType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.EnumType;
import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;
import java.time.OffsetDateTime;
import java.util.UUID;
import com.capstone.smart_parcel.domain.enums.DecisionStatus;
import com.capstone.smart_parcel.domain.enums.DischargeStatus;
import com.capstone.smart_parcel.domain.enums.InputType;

@Entity
@Table(name = "sorting_attempts")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class SortingAttempt {
    @Id
    private UUID attemptId;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "organization_id", nullable = false)
    private Organization organization;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "belt_id", nullable = false)
    private ConveyorBelt belt;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "device_id", nullable = false)
    private Device device;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "rule_version_id", nullable = false)
    private RuleVersion ruleVersion;

    private OffsetDateTime capturedAt;

    private OffsetDateTime decidedAt;
    @Enumerated(EnumType.STRING)
    private DecisionStatus decisionStatus;

    private String decisionErrorCode;
    @Enumerated(EnumType.STRING) @Builder.Default
    private DischargeStatus dischargeStatus = DischargeStatus.UNCONFIRMED;

    private OffsetDateTime dischargeAt;

    private boolean dischargeConflict;
    @Enumerated(EnumType.STRING)
    private InputType recognizedInputType;

    private String recognizedValue;
    @ManyToOne(fetch = FetchType.LAZY, optional = true)
    @JoinColumn(name = "rule_id", nullable = true)
    private SortingRule rule;
    @ManyToOne(fetch = FetchType.LAZY, optional = true)
    @JoinColumn(name = "chute_id", nullable = true)
    private Chute chute;
    @ManyToOne(fetch = FetchType.LAZY, optional = true)
    @JoinColumn(name = "observed_chute_id", nullable = true)
    private Chute observedChute;

    private String groupNameSnapshot;

    private String itemNameSnapshot;

    private String chuteNameSnapshot;

    private String imageUri;
    @Builder.Default @Column(nullable = false, updatable = false)
    private OffsetDateTime createdAt = OffsetDateTime.now();

}

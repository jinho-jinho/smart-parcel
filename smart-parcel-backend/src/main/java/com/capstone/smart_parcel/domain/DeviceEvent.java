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
import com.capstone.smart_parcel.domain.enums.EventType;

@Entity
@Table(name = "device_events")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class DeviceEvent {
    @Id
    private UUID eventId;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "organization_id", nullable = false)
    private Organization organization;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "belt_id", nullable = false)
    private ConveyorBelt belt;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "device_id", nullable = false)
    private Device device;
    @ManyToOne(fetch = FetchType.LAZY, optional = true)
    @JoinColumn(name = "attempt_id", nullable = true)
    private SortingAttempt attempt;
    @Enumerated(EnumType.STRING)
    private EventType eventType;

    private String errorCode;

    private OffsetDateTime occurredAt;
    @Builder.Default
    private OffsetDateTime receivedAt = OffsetDateTime.now();
    @Column(columnDefinition = "char(64)")
    private String payloadSha256;
    @Column(columnDefinition = "char(64)")
    private String imageSha256;

    private String imageUri;
    @org.hibernate.annotations.JdbcTypeCode(org.hibernate.type.SqlTypes.JSON) @Column(columnDefinition = "jsonb")
    private String payload;

}

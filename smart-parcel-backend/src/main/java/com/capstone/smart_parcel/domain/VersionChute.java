package com.capstone.smart_parcel.domain;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.Column;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.FetchType;
import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

@Entity
@Table(name = "version_chutes")
@IdClass(VersionChute.Key.class)
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class VersionChute {
    @Id @Column(name = "organization_id") private Long organizationId;
    @Id @Column(name = "belt_id") private Long beltId;
    @Id @Column(name = "version_id") private Long versionId;
    @Id @Column(name = "chute_id") private Long chuteId;
    private String chuteNameSnapshot;
    private short servoDegSnapshot;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "version_id", insertable = false, updatable = false)
    private RuleVersion version;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "chute_id", insertable = false, updatable = false)
    private Chute chute;

    @Data @NoArgsConstructor @AllArgsConstructor
    public static class Key implements java.io.Serializable {
        private Long organizationId;
        private Long beltId;
        private Long versionId;
        private Long chuteId;
    }
}

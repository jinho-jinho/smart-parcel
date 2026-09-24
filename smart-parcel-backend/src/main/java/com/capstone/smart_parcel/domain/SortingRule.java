package com.capstone.smart_parcel.domain;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.persistence.Id;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
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
import com.capstone.smart_parcel.domain.enums.InputType;

@Entity
@Table(name = "sorting_rules")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class SortingRule {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "organization_id", nullable = false)
    private Organization organization;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "belt_id", nullable = false)
    private ConveyorBelt belt;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "version_id", nullable = false)
    private RuleVersion version;

    private String ruleName;

    private int priority;
    @Enumerated(EnumType.STRING)
    private InputType inputType;

    private String inputValue;

    private String itemName;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "chute_id", nullable = false)
    private Chute chute;

}

package com.capstone.smart_parcel.service;
import com.capstone.smart_parcel.domain.ConveyorBelt;
import com.capstone.smart_parcel.domain.enums.VersionStatus;
import com.capstone.smart_parcel.repository.ConveyorBeltRepository;
import com.capstone.smart_parcel.repository.RuleVersionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Objects;
import static com.capstone.smart_parcel.service.SortingContextService.*;
import static com.capstone.smart_parcel.dto.belt.BeltDtos.*;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class BeltService {
    private final SortingContextService access;
    private final ConveyorBeltRepository belts;
    private final RuleVersionRepository versions;

    public OrganizationResponse organization(String email) {
        var o = access.actor(email, false).getOrganization();
        return new OrganizationResponse(o.getId(), o.getCode(), o.getName());
    }
    public List<BeltResponse> list(String email) {
        return belts.findByOrganization_IdOrderById(access.actor(email, false).getOrganizationId()).stream()
                .map(b -> new BeltResponse(b.getId(), b.getCode(), b.getName(), b.isEnabled(),
                        b.getDesiredRuleVersion() == null ? null : b.getDesiredRuleVersion().getId())).toList();
    }
    @Transactional
    public long createBelt(String email, BeltInput in) {
        var actor = access.actor(email, true);
        return belts.save(ConveyorBelt.builder().organization(actor.getOrganization())
                .code(in.code().trim()).name(in.name().trim()).build()).getId();
    }
    @Transactional
    public void activate(String email, long beltId, long versionId, Long expectedVersionId) {
        long org = access.actor(email, true).getOrganizationId();
        var belt = belts.lockByOrganization(org, beltId).orElseThrow(SortingContextService::notFound);
        Long current = belt.getDesiredRuleVersion() == null ? null : belt.getDesiredRuleVersion().getId();
        if (!Objects.equals(current, expectedVersionId))
            throw conflict("Active configuration changed; reload before activating");
        var version = versions.findByOrganization_IdAndBelt_IdAndId(org, beltId, versionId)
                .orElseThrow(SortingContextService::notFound);
        require(version.getStatus() == VersionStatus.PUBLISHED, "Only published configurations may be activated");
        belt.setDesiredRuleVersion(version);
        belt.setDesiredSetAt(OffsetDateTime.now());
    }
}

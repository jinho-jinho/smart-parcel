package com.capstone.smart_parcel.service;
import com.capstone.smart_parcel.domain.Chute;
import com.capstone.smart_parcel.domain.RuleVersion;
import com.capstone.smart_parcel.domain.SortingRule;
import com.capstone.smart_parcel.domain.VersionChute;
import com.capstone.smart_parcel.domain.enums.VersionStatus;
import com.capstone.smart_parcel.repository.ChuteRepository;
import com.capstone.smart_parcel.repository.ConveyorBeltRepository;
import com.capstone.smart_parcel.repository.MigrationIssueRepository;
import com.capstone.smart_parcel.repository.RuleVersionRepository;
import com.capstone.smart_parcel.repository.SortingRuleRepository;
import com.capstone.smart_parcel.repository.VersionChuteRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.Locale;
import java.util.HashMap;
import static com.capstone.smart_parcel.service.SortingContextService.*;
import static com.capstone.smart_parcel.dto.belt.BeltDtos.*;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class SortingRuleService {
    private final SortingContextService access;
    private final ConveyorBeltRepository belts;
    private final SortingGroupService groups;
    private final ChuteRepository chutes;
    private final RuleVersionRepository versions;
    private final VersionChuteRepository versionChutes;
    private final SortingRuleRepository rules;
    private final MigrationIssueRepository migrationIssues;

    @Transactional
    public long createVersion(String email, long beltId, VersionInput in) {
        long org = access.actor(email, true).getOrganizationId();
        // All configuration writers lock belt -> version -> children.
        var belt = belts.lockByOrganization(org, beltId).orElseThrow(SortingContextService::notFound);
        var group = groups.getOrCreate(belt, in.groupName().trim());
        var version = versions.save(RuleVersion.builder().organization(belt.getOrganization()).belt(belt)
                .group(group).groupNameSnapshot(group.getName()).versionNo(versions.latestNumber(group.getId()) + 1).build());
        Map<Long, Chute> configured = new HashMap<>();
        for (var setting : in.chutes()) {
            require(!configured.containsKey(setting.chuteId()), "Duplicate chute setting");
            var chute = chutes.findByOrganization_IdAndBelt_IdAndId(org, beltId, setting.chuteId())
                    .orElseThrow(SortingContextService::notFound);
            require(chute.isEnabled(), "Chute is disabled");
            configured.put(chute.getId(), chute);
            versionChutes.save(VersionChute.builder().organizationId(org).beltId(beltId).versionId(version.getId())
                    .chuteId(chute.getId()).chuteNameSnapshot(chute.getName()).servoDegSnapshot((short) setting.servoDeg()).build());
        }
        // Flush parent rows before IDENTITY inserts into sorting_rules reference their composite FK.
        versionChutes.flush();
        for (var rule : in.rules()) {
            require(configured.containsKey(rule.chuteId()), "Rule chute is not part of this version");
            rules.save(SortingRule.builder().organization(belt.getOrganization()).belt(belt).version(version)
                    .ruleName(rule.name().trim()).priority(rule.priority()).inputType(rule.inputType())
                    .inputValue(normalize(rule.inputValue())).itemName(rule.itemName().trim())
                    .chute(configured.get(rule.chuteId())).build());
        }
        return version.getId();
    }

    @Transactional
    public ConfigurationResponse publish(String email, long beltId, long versionId) {
        long org = access.actor(email, true).getOrganizationId();
        belts.lockByOrganization(org, beltId).orElseThrow(SortingContextService::notFound);
        var version = versions.lockByScope(org, beltId, versionId).orElseThrow(SortingContextService::notFound);
        if (version.getStatus() != VersionStatus.DRAFT) throw conflict("Version is already published");
        require(rules.countByVersion_Id(versionId) > 0, "At least one rule is required");
        require(!migrationIssues.existsByVersion_Id(versionId), "Imported ambiguous rules require a reviewed replacement version");
        version.setStatus(VersionStatus.PUBLISHED);
        version.setPublishedAt(OffsetDateTime.now());
        versions.flush();
        return configuration(org, beltId, versionId);
    }

    public List<VersionSummary> list(String email, long beltId) {
        long org = access.actor(email, false).getOrganizationId();
        access.belt(org, beltId);
        return versions.findByOrganization_IdAndBelt_IdOrderByIdDesc(org, beltId).stream()
                .map(v -> new VersionSummary(v.getId(), beltId, v.getGroup().getId(), v.getGroupNameSnapshot(),
                        v.getVersionNo(), v.getStatus(), v.getPublishedAt())).toList();
    }
    public ConfigurationResponse get(String email, long beltId, long versionId) {
        return configuration(access.actor(email, false).getOrganizationId(), beltId, versionId);
    }
    public ConfigurationResponse configuration(long org, long beltId, long versionId) {
        var version = versions.findByOrganization_IdAndBelt_IdAndId(org, beltId, versionId)
                .orElseThrow(SortingContextService::notFound);
        var ruleDtos = rules.findByVersion_IdOrderByPriorityAscIdAsc(versionId).stream()
                .map(r -> new RuleResponse(r.getId(), r.getRuleName(), r.getPriority(), r.getInputType(),
                        r.getInputValue(), r.getItemName(), r.getChute().getId())).toList();
        var chuteDtos = versionChutes.findByVersionIdOrderByChuteId(versionId).stream()
                .map(c -> new VersionChuteResponse(c.getChuteId(), c.getChuteNameSnapshot(), c.getServoDegSnapshot())).toList();
        return new ConfigurationResponse(versionId, beltId, version.getGroup().getId(), version.getGroupNameSnapshot(),
                version.getVersionNo(), version.getStatus(), ruleDtos, chuteDtos);
    }
    public static String normalize(String input) { return input.trim().toUpperCase(Locale.ROOT); }
}

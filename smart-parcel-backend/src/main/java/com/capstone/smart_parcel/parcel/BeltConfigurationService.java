package com.capstone.smart_parcel.parcel;

import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.security.SecureRandom;
import java.util.*;
import static com.capstone.smart_parcel.parcel.ParcelAccess.*;
import static com.capstone.smart_parcel.parcel.ParcelDtos.*;

@Service
@RequiredArgsConstructor
public class BeltConfigurationService {
    private final JdbcTemplate jdbc;
    private final ParcelAccess access;
    private final PasswordEncoder passwords;

    @Transactional
    public long createStaff(String email, StaffInput in) {
        var actor=access.actor(email,true);
        return jdbc.queryForObject("""
                INSERT INTO public.users(email,name,password,role,manager_id,organization_id)
                VALUES (?,?,?,'STAFF',?,?) RETURNING id
                """,Long.class,in.email().trim().toLowerCase(Locale.ROOT),in.name().trim(),
                passwords.encode(in.password()),actor.id(),actor.organizationId());
    }
    @Transactional
    public long createBelt(String email, BeltInput in) {
        var actor=access.actor(email,true);
        return jdbc.queryForObject("INSERT INTO parcel.conveyor_belts(organization_id,code,name) VALUES (?,?,?) RETURNING id",
                Long.class,actor.organizationId(),in.code().trim(),in.name().trim());
    }
    @Transactional
    public long createChute(String email,long beltId,ChuteInput in) {
        var actor=access.actor(email,true); access.belt(actor.organizationId(),beltId);
        return jdbc.queryForObject("INSERT INTO parcel.chutes(organization_id,belt_id,code,name) VALUES (?,?,?,?) RETURNING id",
                Long.class,actor.organizationId(),beltId,in.code().trim(),in.name().trim());
    }
    @Transactional
    public DeviceCredential registerDevice(String email,long beltId,DeviceInput in) {
        var actor=access.actor(email,true); access.belt(actor.organizationId(),beltId);
        byte[] random=new byte[32]; new SecureRandom().nextBytes(random);
        String key=Base64.getUrlEncoder().withoutPadding().encodeToString(random);
        UUID id=UUID.randomUUID();
        jdbc.update("INSERT INTO parcel.devices(id,organization_id,belt_id,device_code,credential_hash) VALUES (?,?,?,?,?)",
                id,actor.organizationId(),beltId,in.code().trim(),passwords.encode(key));
        return new DeviceCredential(id,beltId,key);
    }
    @Transactional
    public void deactivateDevice(String email,UUID id,boolean revoke) {
        var actor=access.actor(email,true);
        int changed=jdbc.update("UPDATE parcel.devices SET active=false,credential_revoked=? WHERE organization_id=? AND id=?",
                revoke,actor.organizationId(),id);
        require(changed==1,"Device not found in this organization");
    }
    @Transactional
    public long createVersion(String email,long beltId,VersionInput in) {
        var actor=access.actor(email,true); long org=actor.organizationId();
        // All configuration writers take locks in belt -> version -> child order.
        access.one("SELECT id FROM parcel.conveyor_belts WHERE organization_id=? AND id=? FOR UPDATE",org,beltId);
        jdbc.update("INSERT INTO parcel.sorting_groups(organization_id,belt_id,name) VALUES (?,?,?) ON CONFLICT (belt_id,name) DO NOTHING",
                org,beltId,in.groupName().trim());
        long group=number(access.one("SELECT id FROM parcel.sorting_groups WHERE organization_id=? AND belt_id=? AND name=?",
                org,beltId,in.groupName().trim()),"id");
        long version=jdbc.queryForObject("""
                INSERT INTO parcel.rule_versions(organization_id,belt_id,group_id,group_name_snapshot,version_no)
                SELECT ?,?,?,?,COALESCE(MAX(version_no),0)+1 FROM parcel.rule_versions WHERE group_id=? RETURNING id
                """,Long.class,org,beltId,group,in.groupName().trim(),group);
        Set<Long> configured=new HashSet<>();
        for(var chute:in.chutes()) {
            require(configured.add(chute.chuteId()),"Duplicate chute setting");
            var physical=access.one("SELECT name FROM parcel.chutes WHERE organization_id=? AND belt_id=? AND id=? AND enabled",
                    org,beltId,chute.chuteId());
            jdbc.update("""
                    INSERT INTO parcel.version_chutes(organization_id,belt_id,version_id,chute_id,chute_name_snapshot,servo_deg_snapshot)
                    VALUES (?,?,?,?,?,?)
                    """,org,beltId,version,chute.chuteId(),physical.get("name"),chute.servoDeg());
        }
        for(var rule:in.rules()) {
            require(configured.contains(rule.chuteId()),"Rule chute is not part of this version");
            jdbc.update("""
                    INSERT INTO parcel.sorting_rules(organization_id,belt_id,version_id,rule_name,priority,input_type,input_value,item_name,chute_id)
                    VALUES (?,?,?,?,?,?,?,?,?)
                    """,org,beltId,version,rule.name().trim(),rule.priority(),rule.inputType().name(),
                    normalize(rule.inputValue()),rule.itemName().trim(),rule.chuteId());
        }
        return version;
    }
    @Transactional
    public Map<String,Object> publish(String email,long beltId,long versionId) {
        var actor=access.actor(email,true); long org=actor.organizationId();
        access.one("SELECT id FROM parcel.conveyor_belts WHERE organization_id=? AND id=? FOR UPDATE",org,beltId);
        // Lock BEFORE checking completeness, closing the validation/publication race.
        var version=access.one("SELECT * FROM parcel.rule_versions WHERE organization_id=? AND belt_id=? AND id=? FOR UPDATE",
                org,beltId,versionId);
        if (!"DRAFT".equals(version.get("status"))) throw conflict("Version is already published");
        require(jdbc.queryForObject("SELECT count(*) FROM parcel.sorting_rules WHERE version_id=?",Long.class,versionId)>0,
                "At least one rule is required");
        require(jdbc.queryForObject("SELECT count(*) FROM parcel.migration_issues WHERE version_id=?",Long.class,versionId)==0,
                "Imported ambiguous rules require a newly reviewed version");
        jdbc.update("UPDATE parcel.rule_versions SET status='PUBLISHED' WHERE id=?",versionId);
        return configuration(org,beltId,versionId);
    }
    @Transactional
    public void activate(String email,long beltId,long versionId,Long expectedVersionId) {
        var actor=access.actor(email,true);
        var belt=access.one("SELECT * FROM parcel.conveyor_belts WHERE organization_id=? AND id=? FOR UPDATE",
                actor.organizationId(),beltId);
        Long current=belt.get("desired_rule_version_id")==null ? null : number(belt,"desired_rule_version_id");
        if (!Objects.equals(current,expectedVersionId)) throw conflict("Active configuration changed; reload before activating");
        access.one("SELECT id FROM parcel.rule_versions WHERE organization_id=? AND belt_id=? AND id=? AND status='PUBLISHED'",
                actor.organizationId(),beltId,versionId);
        jdbc.update("UPDATE parcel.conveyor_belts SET desired_rule_version_id=?,desired_set_at=now() WHERE id=?",versionId,beltId);
    }
    @Transactional(readOnly=true)
    public Map<String,Object> configuration(long org,long belt,long version) {
        var config=new LinkedHashMap<>(access.one("SELECT * FROM parcel.rule_versions WHERE organization_id=? AND belt_id=? AND id=?",
                org,belt,version));
        config.put("rules",jdbc.queryForList("SELECT * FROM parcel.sorting_rules WHERE version_id=? ORDER BY priority,id",version));
        config.put("chutes",jdbc.queryForList("SELECT * FROM parcel.version_chutes WHERE version_id=? ORDER BY chute_id",version));
        return config;
    }
    @Transactional(readOnly=true)
    public Map<String,Object> setup(DeviceIdentity device) {
        require(device.active(),"Device is no longer the active controller");
        var belt=access.belt(device.organizationId(),device.beltId());
        require((Boolean)belt.get("enabled"),"Belt is disabled");
        if (belt.get("desired_rule_version_id")==null) throw conflict("No published configuration has been activated");
        return configuration(device.organizationId(),device.beltId(),number(belt,"desired_rule_version_id"));
    }
    @Transactional
    public void applied(DeviceIdentity device,long version) {
        require(device.active(),"Device is no longer the active controller");
        var belt=access.one("SELECT * FROM parcel.conveyor_belts WHERE organization_id=? AND id=? FOR UPDATE",
                device.organizationId(),device.beltId());
        if (belt.get("desired_rule_version_id")==null || number(belt,"desired_rule_version_id")!=version)
            throw conflict("Configuration has changed; fetch setup again");
        jdbc.update("UPDATE parcel.devices SET applied_rule_version_id=?,applied_at=now(),last_seen_at=now() WHERE id=?",
                version,device.id());
    }
    public static String normalize(String input) { return input.trim().toUpperCase(Locale.ROOT); }
}

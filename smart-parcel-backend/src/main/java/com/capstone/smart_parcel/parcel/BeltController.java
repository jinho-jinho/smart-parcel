package com.capstone.smart_parcel.parcel;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.Authentication;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import java.util.*;
import static com.capstone.smart_parcel.parcel.ParcelDtos.*;

@RestController
@RequestMapping("/api/v2")
@RequiredArgsConstructor
@Validated
public class BeltController {
    private final BeltConfigurationService configuration;
    private final ParcelAccess access;
    private final JdbcTemplate jdbc;
    public record Activation(@NotNull Long versionId,Long expectedVersionId) {}
    @PostMapping("/staff")
    public Object staff(Authentication auth,@Valid @RequestBody StaffInput body) {
        return Map.of("id",configuration.createStaff(auth.getName(),body));
    }
    @GetMapping("/organization")
    public Object organization(Authentication auth) {
        var actor=access.actor(auth.getName(),false);
        return access.one("SELECT * FROM parcel.organizations WHERE id=?",actor.organizationId());
    }
    @GetMapping("/belts")
    public Object belts(Authentication auth) {
        return jdbc.queryForList("SELECT * FROM parcel.conveyor_belts WHERE organization_id=? ORDER BY id",
                access.actor(auth.getName(),false).organizationId());
    }
    @PostMapping("/belts")
    public Object belt(Authentication auth,@Valid @RequestBody BeltInput body) {
        return Map.of("id",configuration.createBelt(auth.getName(),body));
    }
    @GetMapping("/belts/{belt}/chutes")
    public Object chutes(Authentication auth,@PathVariable long belt) {
        var actor=access.actor(auth.getName(),false); access.belt(actor.organizationId(),belt);
        return jdbc.queryForList("SELECT * FROM parcel.chutes WHERE organization_id=? AND belt_id=? ORDER BY id",actor.organizationId(),belt);
    }
    @PostMapping("/belts/{belt}/chutes")
    public Object chute(Authentication auth,@PathVariable long belt,@Valid @RequestBody ChuteInput body) {
        return Map.of("id",configuration.createChute(auth.getName(),belt,body));
    }
    @PostMapping("/belts/{belt}/devices")
    public DeviceCredential device(Authentication auth,@PathVariable long belt,@Valid @RequestBody DeviceInput body) {
        return configuration.registerDevice(auth.getName(),belt,body);
    }
    @GetMapping("/belts/{belt}/devices")
    public Object devices(Authentication auth,@PathVariable long belt) {
        var actor=access.actor(auth.getName(),false); access.belt(actor.organizationId(),belt);
        return jdbc.queryForList("""
                SELECT id,device_code,active,credential_revoked,applied_rule_version_id,applied_at,last_seen_at
                FROM parcel.devices WHERE organization_id=? AND belt_id=? ORDER BY created_at
                """,actor.organizationId(),belt);
    }
    @PostMapping("/devices/{id}/deactivate")
    public void deactivate(Authentication auth,@PathVariable UUID id,@RequestParam(defaultValue="false") boolean revoke) {
        configuration.deactivateDevice(auth.getName(),id,revoke);
    }
    @PostMapping("/belts/{belt}/versions")
    public Object version(Authentication auth,@PathVariable long belt,@Valid @RequestBody VersionInput body) {
        return Map.of("id",configuration.createVersion(auth.getName(),belt,body));
    }
    @GetMapping("/belts/{belt}/versions")
    public Object versions(Authentication auth,@PathVariable long belt) {
        var actor=access.actor(auth.getName(),false); access.belt(actor.organizationId(),belt);
        return jdbc.queryForList("SELECT * FROM parcel.rule_versions WHERE organization_id=? AND belt_id=? ORDER BY id DESC",
                actor.organizationId(),belt);
    }
    @GetMapping("/belts/{belt}/versions/{version}")
    public Object version(Authentication auth,@PathVariable long belt,@PathVariable long version) {
        return configuration.configuration(access.actor(auth.getName(),false).organizationId(),belt,version);
    }
    @PostMapping("/belts/{belt}/versions/{version}/publish")
    public Object publish(Authentication auth,@PathVariable long belt,@PathVariable long version) {
        return configuration.publish(auth.getName(),belt,version);
    }
    @PutMapping("/belts/{belt}/active-version")
    public void activate(Authentication auth,@PathVariable long belt,@Valid @RequestBody Activation body) {
        configuration.activate(auth.getName(),belt,body.versionId(),body.expectedVersionId());
    }
}

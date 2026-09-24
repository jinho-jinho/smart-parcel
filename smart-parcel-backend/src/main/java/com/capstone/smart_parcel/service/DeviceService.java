package com.capstone.smart_parcel.service;
import com.capstone.smart_parcel.domain.Device;

import com.capstone.smart_parcel.repository.ConveyorBeltRepository;
import com.capstone.smart_parcel.repository.DeviceRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.OffsetDateTime;
import java.util.UUID;
import java.util.List;
import java.util.Base64;
import static com.capstone.smart_parcel.service.SortingContextService.*;
import static com.capstone.smart_parcel.dto.belt.BeltDtos.*;

import com.capstone.smart_parcel.config.security.DeviceIdentity;
import org.springframework.security.crypto.password.PasswordEncoder;
import java.security.SecureRandom;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class DeviceService {
    private final SortingContextService access;
    private final ConveyorBeltRepository belts;
    private final DeviceRepository devices;
    private final SortingRuleService configurations;
    private final PasswordEncoder passwords;

    @Transactional
    public DeviceCredential registerDevice(String email, long beltId, DeviceInput in) {
        var actor = access.actor(email, true);
        var belt = access.belt(actor.getOrganizationId(), beltId);
        byte[] random = new byte[32];
        new SecureRandom().nextBytes(random);
        String key = Base64.getUrlEncoder().withoutPadding().encodeToString(random);
        UUID id = UUID.randomUUID();
        devices.save(Device.builder().id(id).organization(actor.getOrganization()).belt(belt)
                .deviceCode(in.code().trim()).credentialHash(passwords.encode(key)).build());
        return new DeviceCredential(id, beltId, key);
    }
    public List<DeviceResponse> list(String email, long beltId) {
        long org = access.actor(email, false).getOrganizationId();
        access.belt(org, beltId);
        return devices.findByOrganization_IdAndBelt_IdOrderByCreatedAt(org, beltId).stream()
                .map(d -> new DeviceResponse(d.getId(), d.getDeviceCode(), d.isActive(), d.isCredentialRevoked(),
                        d.getAppliedRuleVersion() == null ? null : d.getAppliedRuleVersion().getId(),
                        d.getAppliedAt(), d.getLastSeenAt())).toList();
    }
    @Transactional
    public void deactivateDevice(String email, UUID id, boolean revoke) {
        var actor = access.actor(email, true);
        var device = devices.findByOrganization_IdAndId(actor.getOrganizationId(), id).orElseThrow(SortingContextService::notFound);
        device.setActive(false);
        // Revocation is irreversible through this endpoint.
        device.setCredentialRevoked(device.isCredentialRevoked() || revoke);
    }
    public ConfigurationResponse setup(DeviceIdentity identity) {
        require(identity.active(), "Device is no longer the active controller");
        var belt = access.belt(identity.organizationId(), identity.beltId());
        require(belt.isEnabled(), "Belt is disabled");
        if (belt.getDesiredRuleVersion() == null) throw conflict("No published configuration has been activated");
        return configurations.configuration(identity.organizationId(), identity.beltId(), belt.getDesiredRuleVersion().getId());
    }
    @Transactional
    public void applied(DeviceIdentity identity, long versionId) {
        require(identity.active(), "Device is no longer the active controller");
        var belt = belts.lockByOrganization(identity.organizationId(), identity.beltId()).orElseThrow(SortingContextService::notFound);
        if (belt.getDesiredRuleVersion() == null || belt.getDesiredRuleVersion().getId() != versionId)
            throw conflict("Configuration has changed; fetch setup again");
        var device = devices.findByOrganization_IdAndId(identity.organizationId(), identity.id()).orElseThrow(SortingContextService::notFound);
        require(device.isActive() && !device.isCredentialRevoked(), "Device is no longer active");
        device.setAppliedRuleVersion(belt.getDesiredRuleVersion());
        device.setAppliedAt(OffsetDateTime.now());
        device.setLastSeenAt(OffsetDateTime.now());
    }
}

package com.capstone.smart_parcel.config.security;
import com.capstone.smart_parcel.repository.DeviceRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class DeviceAuthentication {
    private final DeviceRepository devices;
    private final PasswordEncoder passwords;

    @Transactional(readOnly = true)
    public DeviceIdentity authenticate(String id, String key) {
        if (id == null || key == null || !key.matches("[A-Za-z0-9_-]{43}")) return null;
        UUID uuid;
        try { uuid = UUID.fromString(id); } catch (IllegalArgumentException e) { return null; }
        var device = devices.findByIdAndCredentialRevokedFalse(uuid).orElse(null);
        if (device == null || !passwords.matches(key, device.getCredentialHash())) return null;
        return new DeviceIdentity(uuid, device.getOrganization().getId(), device.getBelt().getId(), device.isActive());
    }
}

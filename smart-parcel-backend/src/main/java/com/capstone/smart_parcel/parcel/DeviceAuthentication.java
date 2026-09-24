package com.capstone.smart_parcel.parcel;

import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class DeviceAuthentication {
    private final JdbcTemplate jdbc;
    private final PasswordEncoder passwords;
    public DeviceIdentity authenticate(String id, String key) {
        if (id == null || key == null || !key.matches("[A-Za-z0-9_-]{43}")) return null;
        UUID uuid;
        try { uuid = UUID.fromString(id); } catch (IllegalArgumentException e) { return null; }
        var rows = jdbc.queryForList("SELECT * FROM parcel.devices WHERE id=? AND NOT credential_revoked",uuid);
        if (rows.isEmpty() || !passwords.matches(key,(String)rows.get(0).get("credential_hash"))) return null;
        var r = rows.get(0);
        return new DeviceIdentity(uuid, ParcelAccess.number(r,"organization_id"), ParcelAccess.number(r,"belt_id"),
                (Boolean)r.get("active"));
    }
}

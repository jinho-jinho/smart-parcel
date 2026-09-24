package com.capstone.smart_parcel.config.security;
import java.util.UUID;
public record DeviceIdentity(UUID id, long organizationId, long beltId, boolean active) {}

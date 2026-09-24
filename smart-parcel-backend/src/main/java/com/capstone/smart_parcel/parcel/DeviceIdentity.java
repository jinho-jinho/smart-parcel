package com.capstone.smart_parcel.parcel;
import java.util.UUID;
public record DeviceIdentity(UUID id, long organizationId, long beltId, boolean active) {}

package com.capstone.smart_parcel.dto.device;

public record DeviceEventResponse(
        Long id,
        String eventType
) {

    public static DeviceEventResponse sorting(Long id) {
        return new DeviceEventResponse(id, "SORTING_RESULT");
    }

    public static DeviceEventResponse error(Long id) {
        return new DeviceEventResponse(id, "ERROR");
    }
}

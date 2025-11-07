package com.capstone.smart_parcel.dto.stats;

public record CountResponse(
        String label,
        Long count
) {
    public static CountResponse of(String label, Long count) {
        return new CountResponse(label, count == null ? 0L : count);
    }
}

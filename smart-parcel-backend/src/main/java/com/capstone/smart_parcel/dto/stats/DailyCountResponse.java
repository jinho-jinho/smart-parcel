package com.capstone.smart_parcel.dto.stats;

import java.time.LocalDate;

public record DailyCountResponse(
        LocalDate date,
        Long count
) {
    public static DailyCountResponse of(LocalDate date, Long count) {
        return new DailyCountResponse(date, count == null ? 0L : count);
    }
}

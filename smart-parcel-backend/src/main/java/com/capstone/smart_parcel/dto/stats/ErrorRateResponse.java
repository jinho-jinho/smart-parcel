package com.capstone.smart_parcel.dto.stats;

public record ErrorRateResponse(
        Long totalProcessed,
        Long totalErrors,
        double errorRatePercent
) {
    public static ErrorRateResponse of(Long totalProcessed, Long totalErrors, double ratePercent) {
        return new ErrorRateResponse(
                totalProcessed == null ? 0L : totalProcessed,
                totalErrors == null ? 0L : totalErrors,
                ratePercent
        );
    }
}

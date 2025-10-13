package com.capstone.smart_parcel.repository.projection;

public interface ErrorRateView {
    Double getErrorRate();     // alias: errorRate (0.0 ~ 1.0)
    Long getTotalProcessed();  // alias: totalProcessed
    Long getTotalErrors();     // alias: totalErrors
}

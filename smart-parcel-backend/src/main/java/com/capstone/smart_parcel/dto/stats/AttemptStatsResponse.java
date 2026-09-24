package com.capstone.smart_parcel.dto.stats;
public record AttemptStatsResponse(long attempts, long matched, long decisionErrors,
                                   long dischargeFailed, long dischargeConflicts) {}

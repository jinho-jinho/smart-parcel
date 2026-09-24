package com.capstone.smart_parcel.dto.history;
import com.capstone.smart_parcel.domain.SortingAttempt;
import com.capstone.smart_parcel.domain.enums.DecisionStatus;
import com.capstone.smart_parcel.domain.enums.DischargeStatus;
import com.capstone.smart_parcel.domain.enums.InputType;
import java.time.OffsetDateTime;
import java.util.UUID;

public record AttemptResponse(UUID attemptId, long beltId, UUID deviceId, long ruleVersionId,
        OffsetDateTime capturedAt, OffsetDateTime decidedAt, DecisionStatus decisionStatus,
        String decisionErrorCode, DischargeStatus dischargeStatus, OffsetDateTime dischargeAt,
        boolean dischargeConflict, InputType recognizedInputType, String recognizedValue,
        Long ruleId, Long chuteId, Long observedChuteId, String groupNameSnapshot,
        String itemNameSnapshot, String chuteNameSnapshot, String imageUri) {
    public static AttemptResponse from(SortingAttempt a) {
        return new AttemptResponse(a.getAttemptId(), a.getBelt().getId(), a.getDevice().getId(),
            a.getRuleVersion().getId(), a.getCapturedAt(), a.getDecidedAt(), a.getDecisionStatus(),
            a.getDecisionErrorCode(), a.getDischargeStatus(), a.getDischargeAt(), a.isDischargeConflict(),
            a.getRecognizedInputType(), a.getRecognizedValue(), a.getRule() == null ? null : a.getRule().getId(),
            a.getChute() == null ? null : a.getChute().getId(), a.getObservedChute() == null ? null : a.getObservedChute().getId(),
            a.getGroupNameSnapshot(), a.getItemNameSnapshot(), a.getChuteNameSnapshot(), a.getImageUri());
    }
}

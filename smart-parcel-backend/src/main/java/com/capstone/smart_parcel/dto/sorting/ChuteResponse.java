package com.capstone.smart_parcel.dto.sorting;

import com.capstone.smart_parcel.domain.Chute;

import java.time.OffsetDateTime;

public record ChuteResponse(
        Long id,
        String chuteName,
        Short servoDeg,
        OffsetDateTime createdAt
) {
    public static ChuteResponse from(Chute chute) {
        return new ChuteResponse(
                chute.getId(),
                chute.getChuteName(),
                chute.getServoDeg(),
                chute.getCreatedAt()
        );
    }
}

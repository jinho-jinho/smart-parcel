package com.capstone.smart_parcel.dto.sorting;

import com.capstone.smart_parcel.domain.SortingRule;
import com.capstone.smart_parcel.domain.enums.InputType;

import java.time.OffsetDateTime;
import java.util.List;

public record SortingRuleResponse(
        Long id,
        Long groupId,
        String ruleName,
        InputType inputType,
        String inputValue,
        String itemName,
        OffsetDateTime createdAt,
        List<ChuteSummary> chutes
) {

    public static SortingRuleResponse from(SortingRule rule, List<ChuteSummary> chutes) {
        return new SortingRuleResponse(
                rule.getId(),
                rule.getGroup() != null ? rule.getGroup().getId() : null,
                rule.getRuleName(),
                rule.getInputType(),
                rule.getInputValue(),
                rule.getItemName(),
                rule.getCreatedAt(),
                chutes == null ? List.of() : List.copyOf(chutes)
        );
    }

    public record ChuteSummary(Long id, String chuteName, Short servoDeg) { }
}

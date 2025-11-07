package com.capstone.smart_parcel.dto.device;

import com.capstone.smart_parcel.domain.RuleChute;
import com.capstone.smart_parcel.domain.SortingGroup;
import com.capstone.smart_parcel.domain.SortingRule;
import com.capstone.smart_parcel.domain.User;
import com.capstone.smart_parcel.domain.enums.InputType;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;

public record DeviceSetupResponse(
        Long managerId,
        Group group,
        List<Rule> rules
) {

    public static DeviceSetupResponse from(User manager,
                                           SortingGroup group,
                                           List<SortingRule> rules,
                                           Map<Long, List<RuleChute>> assignments) {
        List<Rule> ruleSummaries = rules.stream()
                .map(rule -> new Rule(
                        rule.getId(),
                        rule.getRuleName(),
                        rule.getInputType(),
                        rule.getInputValue(),
                        rule.getItemName(),
                        assignments.getOrDefault(rule.getId(), List.of()).stream()
                                .map(rc -> {
                                    var chute = rc.getChute();
                                    return new Chute(
                                            chute.getId(),
                                            chute.getChuteName(),
                                            chute.getServoDeg()
                                    );
                                })
                                .distinct()
                                .toList()
                ))
                .toList();

        return new DeviceSetupResponse(
                manager.getId(),
                new Group(group.getId(), group.getGroupName(), group.getUpdatedAt()),
                List.copyOf(ruleSummaries)
        );
    }

    public record Group(Long id, String name, OffsetDateTime updatedAt) { }

    public record Rule(
            Long id,
            String name,
            InputType inputType,
            String inputValue,
            String itemName,
            List<Chute> chutes
    ) { }

    public record Chute(Long id, String name, Short servoDeg) { }
}

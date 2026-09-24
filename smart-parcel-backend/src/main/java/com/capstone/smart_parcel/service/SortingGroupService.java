package com.capstone.smart_parcel.service;
import com.capstone.smart_parcel.domain.ConveyorBelt;
import com.capstone.smart_parcel.domain.SortingGroup;

import com.capstone.smart_parcel.repository.SortingGroupRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;
import static com.capstone.smart_parcel.service.SortingContextService.*;
import static com.capstone.smart_parcel.dto.belt.BeltDtos.*;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class SortingGroupService {
    private final SortingContextService access;
    private final SortingGroupRepository groups;

    public List<GroupResponse> list(String email, long belt) {
        long org = access.actor(email, false).getOrganizationId();
        access.belt(org, belt);
        return groups.findByOrganization_IdAndBelt_IdOrderById(org, belt).stream()
                .map(g -> new GroupResponse(g.getId(), belt, g.getName())).toList();
    }

    /** The caller must hold the belt lock while creating a configuration. */
    @Transactional
    public SortingGroup getOrCreate(ConveyorBelt belt, String name) {
        return groups.findByBelt_IdAndName(belt.getId(), name).orElseGet(() ->
                groups.save(SortingGroup.builder().organization(belt.getOrganization())
                        .belt(belt).name(name).build()));
    }
}

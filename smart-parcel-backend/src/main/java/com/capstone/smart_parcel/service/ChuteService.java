package com.capstone.smart_parcel.service;
import com.capstone.smart_parcel.domain.Chute;

import com.capstone.smart_parcel.repository.ChuteRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;
import static com.capstone.smart_parcel.service.SortingContextService.*;
import static com.capstone.smart_parcel.dto.belt.BeltDtos.*;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ChuteService {
    private final SortingContextService access;
    private final ChuteRepository chutes;

    public List<ChuteResponse> list(String email, long belt) {
        long org = access.actor(email, false).getOrganizationId();
        access.belt(org, belt);
        return chutes.findByOrganization_IdAndBelt_IdOrderById(org, belt).stream()
                .map(c -> new ChuteResponse(c.getId(), belt, c.getCode(), c.getName(), c.isEnabled())).toList();
    }
    @Transactional
    public long createChute(String email, long belt, ChuteInput in) {
        var actor = access.actor(email, true);
        return chutes.save(Chute.builder().organization(actor.getOrganization())
                .belt(access.belt(actor.getOrganizationId(), belt))
                .code(in.code().trim()).name(in.name().trim()).build()).getId();
    }
}

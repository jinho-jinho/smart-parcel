package com.capstone.smart_parcel.service;

import com.capstone.smart_parcel.domain.Chute;
import com.capstone.smart_parcel.domain.SortingGroup;
import com.capstone.smart_parcel.dto.common.PageResponse;
import com.capstone.smart_parcel.dto.sorting.ChuteCreateRequest;
import com.capstone.smart_parcel.dto.sorting.ChuteResponse;
import com.capstone.smart_parcel.dto.sorting.ChuteUpdateRequest;
import com.capstone.smart_parcel.repository.ChuteRepository;
import com.capstone.smart_parcel.repository.SortingGroupRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.Locale;
import java.util.NoSuchElementException;
import java.util.Objects;

@Service
@RequiredArgsConstructor
public class ChuteService {

    private final ChuteRepository chuteRepository;
    private final SortingGroupRepository sortingGroupRepository;
    private final SortingContextService sortingContextService;

    @Transactional(readOnly = true)
    public PageResponse<ChuteResponse> getChutes(String email,
                                                 Long groupId,
                                                 String keyword,
                                                 Pageable pageable) {
        var ctx = sortingContextService.resolve(email);
        Page<Chute> page;
        String normalized = normalizeKeyword(keyword);
        String pattern = normalized == null ? null : "%" + normalized + "%";

        if (groupId != null) {
            SortingGroup group = sortingGroupRepository.findByIdAndManager_Id(groupId, ctx.manager().getId())
                    .orElseThrow(() -> new NoSuchElementException("Sorting group not found."));
            page = chuteRepository.searchByGroup(ctx.manager().getId(), group.getId(), pattern, pageable);
        } else {
            page = chuteRepository.searchAll(pattern, pageable);
        }

        return PageResponse.of(page, ChuteResponse::from);
    }

    @Transactional
    public ChuteResponse createChute(String email, ChuteCreateRequest request) {
        var ctx = sortingContextService.resolve(email);
        sortingContextService.ensureManager(ctx.actor());

        String name = request.getChuteName().trim();
        Short servoDeg = request.getServoDeg();
        if (chuteRepository.existsByServoDegAndChuteName(servoDeg, name)) {
            throw new IllegalArgumentException("Chute already exists with the same name and servo angle.");
        }

        Chute chute = new Chute();
        chute.setChuteName(name);
        chute.setServoDeg(servoDeg);
        chute.setCreatedAt(OffsetDateTime.now());

        chuteRepository.save(chute);
        return ChuteResponse.from(chute);
    }

    @Transactional
    public ChuteResponse updateChute(String email, Long chuteId, ChuteUpdateRequest request) {
        var ctx = sortingContextService.resolve(email);
        sortingContextService.ensureManager(ctx.actor());

        Chute chute = chuteRepository.findById(chuteId)
                .orElseThrow(() -> new NoSuchElementException("Chute not found."));

        String currentName = chute.getChuteName();
        Short currentServo = chute.getServoDeg();

        String newName = currentName;
        if (request.getChuteName() != null) {
            String name = request.getChuteName().trim();
            if (name.isEmpty()) {
                throw new IllegalArgumentException("Chute name must not be blank.");
            }
            newName = name;
        }

        Short newServo = currentServo;
        if (request.getServoDeg() != null) {
            newServo = request.getServoDeg();
        }

        if (!Objects.equals(currentName, newName) || !Objects.equals(currentServo, newServo)) {
            if (chuteRepository.existsByServoDegAndChuteName(newServo, newName)) {
                throw new IllegalArgumentException("Chute already exists with the same name and servo angle.");
            }
        }

        chute.setChuteName(newName);
        chute.setServoDeg(newServo);

        return ChuteResponse.from(chute);
    }

    @Transactional
    public void deleteChute(String email, Long chuteId) {
        var ctx = sortingContextService.resolve(email);
        sortingContextService.ensureManager(ctx.actor());

        Chute chute = chuteRepository.findById(chuteId)
                .orElseThrow(() -> new NoSuchElementException("Chute not found."));

        chuteRepository.delete(chute);
    }

    private String normalizeKeyword(String keyword) {
        if (keyword == null) {
            return null;
        }
        String value = keyword.trim();
        if (value.isEmpty()) {
            return null;
        }
        return value.toLowerCase(Locale.ROOT);
    }
}

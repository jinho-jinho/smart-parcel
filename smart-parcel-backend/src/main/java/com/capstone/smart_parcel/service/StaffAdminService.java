package com.capstone.smart_parcel.service;

import com.capstone.smart_parcel.domain.User;
import com.capstone.smart_parcel.dto.common.PageResponse;
import com.capstone.smart_parcel.dto.user.StaffSummaryResponse;
import com.capstone.smart_parcel.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Locale;
import java.util.NoSuchElementException;

@Service
@RequiredArgsConstructor
public class StaffAdminService {

    private final SortingContextService sortingContextService;
    private final UserRepository userRepository;

    @Transactional(readOnly = true)
    public PageResponse<StaffSummaryResponse> listStaff(String email, String keyword, Pageable pageable) {
        var ctx = sortingContextService.resolve(email);
        sortingContextService.ensureManager(ctx.actor());

        String normalized = normalizeKeyword(keyword);
        Page<User> page = userRepository.searchStaffByManager(
                ctx.manager().getId(),
                normalized == null ? null : "%" + normalized + "%",
                pageable
        );
        return PageResponse.of(page, StaffSummaryResponse::from);
    }

    @Transactional
    public void deleteStaff(String email, Long staffId) {
        var ctx = sortingContextService.resolve(email);
        sortingContextService.ensureManager(ctx.actor());

        User staff = userRepository.findByIdAndManager_Id(staffId, ctx.manager().getId())
                .orElseThrow(() -> new NoSuchElementException("직원 정보를 찾을 수 없습니다."));
        userRepository.delete(staff);
    }

    private String normalizeKeyword(String keyword) {
        if (keyword == null) {
            return null;
        }
        String trimmed = keyword.trim();
        if (trimmed.isEmpty()) {
            return null;
        }
        return trimmed.toLowerCase(Locale.ROOT);
    }
}

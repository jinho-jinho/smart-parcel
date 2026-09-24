package com.capstone.smart_parcel.service;
import com.capstone.smart_parcel.domain.User;
import com.capstone.smart_parcel.domain.enums.Role;
import com.capstone.smart_parcel.dto.belt.BeltDtos.StaffInput;
import com.capstone.smart_parcel.dto.common.PageResponse;
import com.capstone.smart_parcel.dto.user.StaffSummaryResponse;
import com.capstone.smart_parcel.repository.UserNotificationRepository;
import com.capstone.smart_parcel.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.Locale;

@Service
@RequiredArgsConstructor
public class StaffAdminService {
    private final SortingContextService access;
    private final UserRepository users;
    private final UserNotificationRepository notifications;
    private final PasswordEncoder passwords;

    @Transactional
    public long createStaff(String email, StaffInput in) {
        var actor = access.actor(email, true);
        return users.save(User.builder().email(in.email().trim().toLowerCase(Locale.ROOT)).name(in.name().trim())
                .password(passwords.encode(in.password())).role(Role.STAFF).createdAt(java.time.OffsetDateTime.now())
                .organization(actor.getOrganization()).manager(actor).build()).getId();
    }
    @Transactional(readOnly = true)
    public PageResponse<StaffSummaryResponse> listStaff(String email, String keyword, Pageable page) {
        var actor = access.actor(email, true);
        String q = keyword == null || keyword.isBlank() ? null : "%" + keyword.trim().toLowerCase(Locale.ROOT) + "%";
        return PageResponse.of(users.searchStaffByOrganization(actor.getOrganizationId(), q,
                SortingHistoryService.page(page.getPageNumber(), page.getPageSize(), "createdAt", "id")), StaffSummaryResponse::from);
    }
    @Transactional
    public void deleteStaff(String email, Long id) {
        var actor = access.actor(email, true);
        var staff = users.findByIdAndOrganization_IdAndRole(id, actor.getOrganizationId(), Role.STAFF)
                .orElseThrow(SortingContextService::notFound);
        notifications.deleteByRecipient_Id(id);
        users.delete(staff);
    }
}

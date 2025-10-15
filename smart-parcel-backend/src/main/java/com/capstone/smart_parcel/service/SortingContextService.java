package com.capstone.smart_parcel.service;

import com.capstone.smart_parcel.domain.User;
import com.capstone.smart_parcel.domain.enums.Role;
import com.capstone.smart_parcel.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class SortingContextService {

    private final UserRepository userRepository;

    @Transactional(readOnly = true)
    public ManagerContext resolve(String email) {
        if (email == null || email.isBlank()) {
            throw new AccessDeniedException("인증 정보가 유효하지 않습니다.");
        }
        String normalized = email.trim().toLowerCase();
        User actor = userRepository.findByEmail(normalized)
                .orElseThrow(() -> new AccessDeniedException("사용자를 찾을 수 없습니다."));
        User manager = actor.getRole() == Role.MANAGER ? actor : actor.getManager();
        if (manager == null) {
            throw new AccessDeniedException("관리자 계정이 연결되어 있지 않습니다.");
        }
        return new ManagerContext(actor, manager);
    }

    public void ensureManager(User actor) {
        if (actor.getRole() != Role.MANAGER) {
            throw new AccessDeniedException("관리자만 사용할 수 있는 기능입니다.");
        }
    }

    public record ManagerContext(User actor, User manager) { }
}

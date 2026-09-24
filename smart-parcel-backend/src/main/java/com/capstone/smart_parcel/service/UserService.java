package com.capstone.smart_parcel.service;

import com.capstone.smart_parcel.domain.User;
import com.capstone.smart_parcel.domain.enums.Role;
import com.capstone.smart_parcel.dto.UserLoginRequestDto;
import com.capstone.smart_parcel.dto.UserResponseDto;
import com.capstone.smart_parcel.dto.UserSignupRequestDto;
import com.capstone.smart_parcel.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final com.capstone.smart_parcel.repository.OrganizationRepository organizations;

    private static String normalize(String s) {
        if (s == null) throw new IllegalArgumentException("값이 비었습니다.");
        String v = s.trim();
        if (v.isEmpty()) throw new IllegalArgumentException("값이 비었습니다.");
        return v.toLowerCase(java.util.Locale.ROOT);
    }

    /** 회원가입 */
    @Transactional
    public Long signup(UserSignupRequestDto dto) {
        final String email = normalize(dto.getEmail());
        final String name  = dto.getName() == null ? null : dto.getName().trim();
        if (name == null || name.isEmpty()) {
            throw new IllegalArgumentException("이름을 입력해 주세요.");
        }
        if (dto.getPassword() == null || dto.getPassword().length() < 8) {
            throw new IllegalArgumentException("비밀번호는 8자 이상이어야 합니다.");
        }

        // 기본권한 보정
        Role role = dto.getRole() == null ? Role.STAFF : dto.getRole();

        // Public signup may create an organization, but cannot self-enroll in someone else's organization.
        if (role == Role.STAFF) {
            throw new IllegalArgumentException("Staff accounts must be created by their organization manager");
        }
        User user = new User();
        user.setEmail(email);
        user.setName(name);
        user.setBizNumber(dto.getBizNumber());
        user.setRole(role);
        user.setPassword(passwordEncoder.encode(dto.getPassword()));
        user.setManager(null);

        user.setOrganization(organizations.save(com.capstone.smart_parcel.domain.Organization.builder()
                .code(java.util.UUID.randomUUID().toString()).name(name).build()));
        try {
            userRepository.save(user); // uq_users_email 최종 방어
        } catch (DataIntegrityViolationException e) {
            throw new IllegalArgumentException("이미 사용 중인 이메일입니다.");
        }
        return user.getId();
    }

    /** 로그인: 검증 + DTO 반환 (토큰 발급은 상위 레이어) */
    @Transactional(readOnly = true)
    public UserResponseDto login(UserLoginRequestDto dto) {
        final String email = normalize(dto.getEmail());
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 이메일입니다."));
        if (dto.getPassword() == null || !passwordEncoder.matches(dto.getPassword(), user.getPassword())) {
            throw new IllegalArgumentException("비밀번호가 일치하지 않습니다.");
        }
        return toDto(user);
    }

    /** id로 사용자 조회 */
    @Transactional(readOnly = true)
    public UserResponseDto getUser(Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));
        return toDto(user);
    }

    /** /user/me: 이메일로 현재 사용자 조회 */
    @Transactional(readOnly = true)
    public UserResponseDto getByEmail(String rawEmail) {
        final String email = normalize(rawEmail);
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));
        return toDto(user);
    }

    /** 비밀번호 변경 */
    @Transactional
    public void changePasswordByEmail(String rawEmail, String newPassword){
        final String email = normalize(rawEmail);
        if (newPassword == null || newPassword.length() < 6) {
            throw new IllegalArgumentException("비밀번호는 6자 이상이어야 합니다.");
        }
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));
        user.setPassword(passwordEncoder.encode(newPassword));
    }

    private static UserResponseDto toDto(User user) {
        return new UserResponseDto(
                user.getId(),
                user.getEmail(),
                user.getName(),
                user.getRole(),
                user.getCreatedAt()
        );
    }
}

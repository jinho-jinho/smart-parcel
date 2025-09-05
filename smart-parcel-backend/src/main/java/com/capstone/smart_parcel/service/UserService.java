package com.capstone.smart_parcel.service;

import com.capstone.smart_parcel.domain.User;
import com.capstone.smart_parcel.domain.UserRole;
import com.capstone.smart_parcel.dto.UserLoginRequestDto;
import com.capstone.smart_parcel.dto.UserResponseDto;
import com.capstone.smart_parcel.dto.UserSignupRequestDto;
import com.capstone.smart_parcel.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    private static String normalize(String email) {
        return email == null ? null : email.trim().toLowerCase();
    }

    /** 회원가입 */
    @Transactional
    public Long signup(UserSignupRequestDto dto) {
        String email = normalize(dto.getEmail());
        if (userRepository.existsByEmail(email)) {
            throw new IllegalArgumentException("이미 사용 중인 이메일입니다.");
        }

        User user = new User();
        user.setEmail(email);
        user.setName(dto.getName());
        user.setBizNumber(dto.getBizNumber());
        user.setRole(UserRole.STAFF); // 기본 권한
        user.setPassword(passwordEncoder.encode(dto.getPassword()));

        userRepository.save(user);
        return user.getId();
    }

    /** 로그인: 이메일/비번 검증만 수행 (토큰 발급은 컨트롤러에서) */
    @Transactional(readOnly = true)
    public User login(UserLoginRequestDto dto) {
        String email = normalize(dto.getEmail());
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 이메일입니다."));

        if (!passwordEncoder.matches(dto.getPassword(), user.getPassword())) {
            throw new IllegalArgumentException("비밀번호가 일치하지 않습니다.");
        }
        return user;
    }

    /** id로 사용자 조회 (관리자/디버그 용) */
    @Transactional(readOnly = true)
    public UserResponseDto getUser(Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));
        return toDto(user);
    }

    /** /user/me 용: 이메일로 현재 사용자 조회 */
    @Transactional(readOnly = true)
    public UserResponseDto getByEmail(String rawEmail) {
        String email = normalize(rawEmail);
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));
        return toDto(user);
    }

    @Transactional
    public void changePasswordByEmail(String rawEmail, String newPassword){
        String email = normalize(rawEmail);
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));
        user.setPassword(passwordEncoder.encode(newPassword));
    }

    private static UserResponseDto toDto(User user) {
        return new UserResponseDto(
                user.getId(),
                user.getEmail(),
                user.getName(),
                user.getCreatedAt()
        );
    }
}

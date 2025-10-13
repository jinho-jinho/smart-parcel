package com.capstone.smart_parcel.repository;


import com.capstone.smart_parcel.domain.User;
import com.capstone.smart_parcel.domain.enums.Role;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {

    // 로그인/회원가입
    Optional<User> findByEmail(String email);
    boolean existsByEmail(String email);
    Optional<User> findByEmailAndRole(String email, Role role);

    // 직원 조회 (관리자 기준)
    Page<User> findByManager_Id(Long managerId, Pageable pageable);
    List<User> findByManager_IdAndRole(Long managerId, Role role);

    // 내 정보 조회
    Optional<User> findById(Long id);
}
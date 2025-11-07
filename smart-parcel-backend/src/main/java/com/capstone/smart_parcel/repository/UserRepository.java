package com.capstone.smart_parcel.repository;

import com.capstone.smart_parcel.domain.User;
import com.capstone.smart_parcel.domain.enums.Role;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByEmail(String email);

    boolean existsByEmail(String email);

    Optional<User> findByEmailAndRole(String email, Role role);

    Page<User> findByManager_Id(Long managerId, Pageable pageable);

    List<User> findByManager_IdAndRole(Long managerId, Role role);

    Optional<User> findById(Long id);

    Optional<User> findByIdAndManager_Id(Long id, Long managerId);

    @Query("""
            SELECT u
            FROM User u
            WHERE u.manager.id = :managerId
              AND (:keyword IS NULL
                   OR lower(u.name) LIKE :keyword
                   OR lower(u.email) LIKE :keyword)
            """)
    Page<User> searchStaffByManager(@Param("managerId") Long managerId,
                                    @Param("keyword") String keyword,
                                    Pageable pageable);
}

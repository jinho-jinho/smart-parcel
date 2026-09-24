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
    List<User> findByOrganization_Id(long organizationId);
    Optional<User> findByIdAndOrganization_IdAndRole(Long id, Long organizationId, Role role);

    @Query("select u from User u where u.organization.id = :org and u.role = 'STAFF' and (:keyword is null or lower(u.name) like :keyword or lower(u.email) like :keyword)")
    Page<User> searchStaffByOrganization(@Param("org") long org, @Param("keyword") String keyword, Pageable pageable);

    boolean existsByEmail(String email);

    Optional<User> findByEmailAndRole(String email, Role role);

}

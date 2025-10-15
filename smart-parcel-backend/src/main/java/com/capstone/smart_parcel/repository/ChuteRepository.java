package com.capstone.smart_parcel.repository;

import com.capstone.smart_parcel.domain.Chute;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface ChuteRepository extends JpaRepository<Chute, Long> {

    Optional<Chute> findByServoDegAndChuteName(Short servoDeg, String chuteName);

    Optional<Chute> findFirstByServoDeg(Short servoDeg);

    boolean existsByServoDegAndChuteName(Short servoDeg, String chuteName);
    @Query("""
      SELECT c
      FROM Chute c
      WHERE (
          coalesce(:keyword, '') = ''
          OR lower(c.chuteName) LIKE concat('%', lower(:keyword), '%')
      )
    """)
    Page<Chute> searchAll(@Param("keyword") String keyword, Pageable pageable);

    @Query("""
      SELECT DISTINCT c
      FROM Chute c
      JOIN RuleChute rc ON rc.chute = c
      JOIN rc.rule r
      JOIN r.group g
      WHERE g.manager.id = :managerId
        AND g.id = :groupId
        AND (
            coalesce(:keyword, '') = ''
            OR lower(c.chuteName) LIKE concat('%', lower(:keyword), '%')
        )
    """)
    Page<Chute> searchByGroup(@Param("managerId") Long managerId,
                              @Param("groupId") Long groupId,
                              @Param("keyword") String keyword,
                              Pageable pageable);
    
}

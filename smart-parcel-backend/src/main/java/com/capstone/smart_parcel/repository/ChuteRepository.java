package com.capstone.smart_parcel.repository;

import com.capstone.smart_parcel.domain.Chute;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ChuteRepository extends JpaRepository<Chute, Long> {

    Optional<Chute> findByServoDegAndChuteName(Short servoDeg, String chuteName);

    // 각도만으로 찾는 유틸 (이름이 고정이거나, 각도만으로 사용하는 시나리오)
    Optional<Chute> findFirstByServoDeg(Short servoDeg);

    boolean existsByServoDegAndChuteName(Short servoDeg, String chuteName);
}

package com.capstone.smart_parcel.repository;
import com.capstone.smart_parcel.domain.Chute;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ChuteRepository extends JpaRepository<Chute, Long> {

    List<Chute> findByOrganization_IdAndBelt_IdOrderById(long org, long belt);
    Optional<Chute> findByOrganization_IdAndBelt_IdAndId(long org, long belt, long id);

}

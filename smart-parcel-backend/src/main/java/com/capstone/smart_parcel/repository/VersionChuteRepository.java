package com.capstone.smart_parcel.repository;
import com.capstone.smart_parcel.domain.VersionChute;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface VersionChuteRepository extends JpaRepository<VersionChute, VersionChute.Key> {

    List<VersionChute> findByVersionIdOrderByChuteId(long version);
    Optional<VersionChute> findByVersionIdAndChuteId(long version, long chute);

}

package com.capstone.smart_parcel.repository;
import com.capstone.smart_parcel.domain.EventImage;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface EventImageRepository extends JpaRepository<EventImage, UUID> {

}

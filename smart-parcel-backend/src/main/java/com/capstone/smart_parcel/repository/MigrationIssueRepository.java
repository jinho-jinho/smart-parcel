package com.capstone.smart_parcel.repository;
import com.capstone.smart_parcel.domain.MigrationIssue;

import org.springframework.data.jpa.repository.JpaRepository;

public interface MigrationIssueRepository extends JpaRepository<MigrationIssue, Long> {

    boolean existsByVersion_Id(long version);

}

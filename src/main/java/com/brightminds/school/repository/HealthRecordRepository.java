package com.brightminds.school.repository;

import com.brightminds.school.entity.HealthRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.UUID;

public interface HealthRecordRepository extends JpaRepository<HealthRecord, UUID> {
    List<HealthRecord> findByPupilIdOrderByVisitDateDesc(UUID pupilId);
}

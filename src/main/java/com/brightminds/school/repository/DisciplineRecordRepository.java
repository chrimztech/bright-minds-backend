package com.brightminds.school.repository;

import com.brightminds.school.entity.DisciplineRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.UUID;

public interface DisciplineRecordRepository extends JpaRepository<DisciplineRecord, UUID> {
    List<DisciplineRecord> findByPupilIdOrderByOccurredOnDesc(UUID pupilId);
}

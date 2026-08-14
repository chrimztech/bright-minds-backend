package com.brightminds.school.repository;

import com.brightminds.school.entity.ReportCardRemark;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;
import java.util.UUID;

public interface ReportCardRemarkRepository extends JpaRepository<ReportCardRemark, UUID> {
    Optional<ReportCardRemark> findByPupilIdAndExamId(UUID pupilId, UUID examId);
}

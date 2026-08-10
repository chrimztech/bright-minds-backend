package com.brightminds.school.repository;

import com.brightminds.school.entity.PupilEnrollment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PupilEnrollmentRepository extends JpaRepository<PupilEnrollment, UUID> {
    Optional<PupilEnrollment> findFirstByPupilIdAndEndedOnIsNullOrderByStartedOnDesc(UUID pupilId);
    Optional<PupilEnrollment> findFirstByPupilIdAndAcademicYearIdOrderByStartedOnDesc(UUID pupilId, UUID academicYearId);
    Optional<PupilEnrollment> findFirstByPupilIdAndStartedOnLessThanEqualOrderByStartedOnDesc(UUID pupilId, java.time.LocalDate onDate);
    List<PupilEnrollment> findByPupilIdOrderByStartedOnDesc(UUID pupilId);
}

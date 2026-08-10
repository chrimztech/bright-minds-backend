package com.brightminds.school.repository;

import com.brightminds.school.entity.Pupil;
import com.brightminds.school.entity.enums.PupilStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PupilRepository extends JpaRepository<Pupil, UUID> {
    Optional<Pupil> findByAdmissionNo(String admissionNo);
    boolean existsByAdmissionNo(String admissionNo);
    List<Pupil> findByStatus(PupilStatus status);
    List<Pupil> findBySchoolClassId(UUID classId);
    long countBySchoolClassId(UUID classId);

    @Query("SELECT p FROM Pupil p WHERE LOWER(p.fullName) LIKE LOWER(CONCAT('%',:q,'%')) OR LOWER(p.admissionNo) LIKE LOWER(CONCAT('%',:q,'%'))")
    Page<Pupil> search(String q, Pageable pageable);

    long countByStatus(PupilStatus status);
}

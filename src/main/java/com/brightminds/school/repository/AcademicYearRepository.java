package com.brightminds.school.repository;

import com.brightminds.school.entity.AcademicYear;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;
import java.util.UUID;

public interface AcademicYearRepository extends JpaRepository<AcademicYear, UUID> {
    Optional<AcademicYear> findByIsCurrentTrue();
}

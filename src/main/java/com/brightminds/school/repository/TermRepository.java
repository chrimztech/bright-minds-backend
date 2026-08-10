package com.brightminds.school.repository;

import com.brightminds.school.entity.Term;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface TermRepository extends JpaRepository<Term, UUID> {
    Optional<Term> findByIsCurrentTrue();
    List<Term> findByAcademicYearId(UUID academicYearId);
}

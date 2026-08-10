package com.brightminds.school.repository;

import com.brightminds.school.entity.Exam;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.UUID;

public interface ExamRepository extends JpaRepository<Exam, UUID> {
    List<Exam> findByTerm_IdOrderByExamDateDesc(UUID termId);
    List<Exam> findAllByOrderByExamDateDesc();
}

package com.brightminds.school.repository;

import com.brightminds.school.entity.Mark;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface MarkRepository extends JpaRepository<Mark, UUID> {
    List<Mark> findByExamId(UUID examId);
    List<Mark> findByPupilId(UUID pupilId);
    List<Mark> findByPupilIdAndExamId(UUID pupilId, UUID examId);
    List<Mark> findByPupilIdInAndExamId(List<UUID> pupilIds, UUID examId);
    Optional<Mark> findByPupilIdAndExamIdAndSubjectId(UUID pupilId, UUID examId, UUID subjectId);

    @Query("""
            SELECT m FROM Mark m
            JOIN FETCH m.exam e
            LEFT JOIN FETCH e.term t
            LEFT JOIN FETCH t.academicYear
            JOIN FETCH m.subject
            WHERE m.pupil.id = :pupilId
            ORDER BY e.examDate DESC, e.name, m.subject.name
            """)
    List<Mark> findReportCardMarks(@Param("pupilId") UUID pupilId);

    @Query("""
            SELECT m FROM Mark m
            JOIN FETCH m.exam e
            JOIN FETCH m.subject
            WHERE e.term.id = :termId AND m.pupil.schoolClass.id = :classId
            """)
    List<Mark> findByTermIdAndClassId(@Param("termId") UUID termId, @Param("classId") UUID classId);
}

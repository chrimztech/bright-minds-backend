package com.brightminds.school.controller;

import com.brightminds.school.entity.Exam;
import com.brightminds.school.entity.Mark;
import com.brightminds.school.entity.Pupil;
import com.brightminds.school.entity.Subject;
import com.brightminds.school.entity.enums.AssessmentType;
import com.brightminds.school.repository.*;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.persistence.EntityNotFoundException;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/exams")
@RequiredArgsConstructor
@Tag(name = "Exams & Marks")
@PreAuthorize("@perm.has('exams:manage')")
public class ExamController {

    private final ExamRepository examRepo;
    private final MarkRepository markRepo;
    private final PupilRepository pupilRepo;
    private final SubjectRepository subjectRepo;
    private final TermRepository termRepo;

    @GetMapping
    public List<Exam> list(@RequestParam(required = false) UUID termId) {
        if (termId != null) return examRepo.findByTerm_IdOrderByExamDateDesc(termId);
        return examRepo.findAllByOrderByExamDateDesc();
    }

    @GetMapping("/{id}")
    public Exam getById(@PathVariable UUID id) {
        return examRepo.findById(id).orElseThrow(() -> new EntityNotFoundException("Exam not found"));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Exam create(@RequestBody ExamRequest req) {
        Exam exam = Exam.builder()
                .name(req.getName())
                .examDate(req.getExamDate())
                .outOf(req.getOutOf() > 0 ? req.getOutOf() : 100)
                .weight(req.getWeight() > 0 ? req.getWeight() : 1.0)
                .assessmentType(req.getAssessmentType())
                .build();
        if (req.getTermId() != null) {
            termRepo.findById(req.getTermId()).ifPresent(exam::setTerm);
        }
        return examRepo.save(exam);
    }

    @PutMapping("/{id}")
    public Exam update(@PathVariable UUID id, @RequestBody ExamRequest req) {
        Exam exam = examRepo.findById(id).orElseThrow(() -> new EntityNotFoundException("Exam not found"));
        exam.setName(req.getName());
        exam.setExamDate(req.getExamDate());
        if (req.getOutOf() > 0) exam.setOutOf(req.getOutOf());
        if (req.getWeight() > 0) exam.setWeight(req.getWeight());
        return examRepo.save(exam);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable UUID id) { examRepo.deleteById(id); }

    // Marks endpoints
    @GetMapping("/{examId}/marks")
    public List<Mark> getMarks(@PathVariable UUID examId) {
        return markRepo.findByExamId(examId);
    }

    @PostMapping("/{examId}/marks")
    @ResponseStatus(HttpStatus.CREATED)
    public Mark addMark(@PathVariable UUID examId, @RequestBody MarkRequest req) {
        Exam exam = examRepo.findById(examId).orElseThrow(() -> new EntityNotFoundException("Exam not found"));
        Pupil pupil = pupilRepo.findById(req.getPupilId()).orElseThrow(() -> new EntityNotFoundException("Pupil not found"));
        Subject subject = subjectRepo.findById(req.getSubjectId()).orElseThrow(() -> new EntityNotFoundException("Subject not found"));

        Mark mark = markRepo.findByPupilIdAndExamIdAndSubjectId(pupil.getId(), examId, subject.getId())
                .orElse(new Mark());
        mark.setPupil(pupil);
        mark.setExam(exam);
        mark.setSubject(subject);
        mark.setScore(req.getScore());
        mark.setComment(req.getComment());
        return markRepo.save(mark);
    }

    @Data
    public static class ExamRequest {
        private String name;
        private UUID termId;
        private AssessmentType assessmentType;
        private LocalDate examDate;
        private int outOf;
        private double weight;
    }

    @Data
    public static class MarkRequest {
        private UUID pupilId;
        private UUID subjectId;
        private double score;
        private String comment;
    }
}

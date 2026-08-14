package com.brightminds.school.controller;

import com.brightminds.school.entity.Exam;
import com.brightminds.school.entity.Pupil;
import com.brightminds.school.entity.ReportCardRemark;
import com.brightminds.school.repository.ExamRepository;
import com.brightminds.school.repository.PupilRepository;
import com.brightminds.school.repository.ReportCardRemarkRepository;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.persistence.EntityNotFoundException;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/report-cards/remarks")
@RequiredArgsConstructor
@Tag(name = "Report Card Remarks")
@PreAuthorize("hasAnyRole('SUPER_ADMIN','ADMIN','HEAD_TEACHER','DEPUTY_HEAD','TEACHER','CLASS_TEACHER')")
public class ReportCardController {

    private final ReportCardRemarkRepository remarkRepo;
    private final PupilRepository pupilRepo;
    private final ExamRepository examRepo;

    @GetMapping
    public ReportCardRemark get(@RequestParam UUID pupilId, @RequestParam UUID examId) {
        return remarkRepo.findByPupilIdAndExamId(pupilId, examId).orElse(null);
    }

    @PutMapping
    public ReportCardRemark save(@RequestBody RemarkRequest req) {
        ReportCardRemark remark = remarkRepo.findByPupilIdAndExamId(req.getPupilId(), req.getExamId())
                .orElseGet(() -> {
                    Pupil pupil = pupilRepo.findById(req.getPupilId())
                            .orElseThrow(() -> new EntityNotFoundException("Pupil not found"));
                    Exam exam = examRepo.findById(req.getExamId())
                            .orElseThrow(() -> new EntityNotFoundException("Exam not found"));
                    return ReportCardRemark.builder().pupil(pupil).exam(exam).build();
                });
        remark.setClassTeacherRemark(req.getClassTeacherRemark());
        remark.setHeadTeacherRemark(req.getHeadTeacherRemark());
        return remarkRepo.save(remark);
    }

    @Data
    public static class RemarkRequest {
        private UUID pupilId;
        private UUID examId;
        private String classTeacherRemark;
        private String headTeacherRemark;
    }
}

package com.brightminds.school.controller;

import com.brightminds.school.entity.Exam;
import com.brightminds.school.entity.Pupil;
import com.brightminds.school.entity.ReportCardRemark;
import com.brightminds.school.repository.ExamRepository;
import com.brightminds.school.repository.PupilRepository;
import com.brightminds.school.repository.ReportCardRemarkRepository;
import com.brightminds.school.service.ClassScopeService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.persistence.EntityNotFoundException;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.Set;
import java.util.UUID;

@RestController
@RequestMapping("/report-cards/remarks")
@RequiredArgsConstructor
@Tag(name = "Report Card Remarks")
@PreAuthorize("@perm.has('report_cards:view')")
public class ReportCardController {

    private final ReportCardRemarkRepository remarkRepo;
    private final PupilRepository pupilRepo;
    private final ExamRepository examRepo;
    private final ClassScopeService scopeService;

    @GetMapping
    public ReportCardRemark get(@RequestParam UUID pupilId, @RequestParam UUID examId, Authentication auth) {
        assertPupilInScope(pupilId, auth);
        return remarkRepo.findByPupilIdAndExamId(pupilId, examId).orElse(null);
    }

    @PutMapping
    public ReportCardRemark save(@RequestBody RemarkRequest req, Authentication auth) {
        assertPupilInScope(req.getPupilId(), auth);
        ReportCardRemark remark = remarkRepo.findByPupilIdAndExamId(req.getPupilId(), req.getExamId())
                .orElseGet(() -> {
                    Pupil pupil = pupilRepo.findById(req.getPupilId())
                            .orElseThrow(() -> new EntityNotFoundException("Pupil not found"));
                    Exam exam = examRepo.findById(req.getExamId())
                            .orElseThrow(() -> new EntityNotFoundException("Exam not found"));
                    return ReportCardRemark.builder().pupil(pupil).exam(exam).build();
                });
        // A class-scoped teacher is the "class teacher" for their own class's pupils, so their
        // remark is theirs to set — but nothing stopped them from also overwriting the head
        // teacher's remark, which defeats the point of having two separately-attributed
        // signatures on the report card. Only an unrestricted (admin-tier) caller may set it;
        // a restricted caller's value for that field is silently dropped, not just left blank,
        // so a class teacher can't accidentally clear a head teacher's existing remark either.
        remark.setClassTeacherRemark(req.getClassTeacherRemark());
        if (!scopeService.isRestricted(auth)) {
            remark.setHeadTeacherRemark(req.getHeadTeacherRemark());
        }
        return remarkRepo.save(remark);
    }

    private void assertPupilInScope(UUID pupilId, Authentication auth) {
        Set<UUID> scope = scopeService.restrictedClassIds(auth);
        if (scope == null) return;
        Pupil pupil = pupilRepo.findById(pupilId).orElseThrow(() -> new EntityNotFoundException("Pupil not found"));
        scopeService.assertInScope(scope, pupil.getSchoolClass() != null ? pupil.getSchoolClass().getId() : null);
    }

    @Data
    public static class RemarkRequest {
        private UUID pupilId;
        private UUID examId;
        private String classTeacherRemark;
        private String headTeacherRemark;
    }
}

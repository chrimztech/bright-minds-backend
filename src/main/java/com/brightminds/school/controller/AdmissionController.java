package com.brightminds.school.controller;

import com.brightminds.school.dto.PupilRequest;
import com.brightminds.school.entity.Admission;
import com.brightminds.school.entity.Pupil;
import com.brightminds.school.entity.enums.AdmissionStatus;
import com.brightminds.school.repository.AdmissionRepository;
import com.brightminds.school.repository.SchoolClassRepository;
import com.brightminds.school.service.PupilService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.persistence.EntityNotFoundException;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

@RestController
@RequestMapping("/admissions")
@RequiredArgsConstructor
@Tag(name = "Admissions")
@PreAuthorize("@perm.has('admissions:manage')")
public class AdmissionController {

    private final AdmissionRepository repo;
    private final SchoolClassRepository classRepo;
    private final PupilService pupilService;

    @PreAuthorize("@perm.has('admissions:view')")
    @GetMapping
    public List<Admission> list(@RequestParam(required = false) String status) {
        if (status != null) return repo.findByStatus(AdmissionStatus.valueOf(status.toUpperCase()));
        return repo.findAll();
    }

    @PreAuthorize("@perm.has('admissions:view')")
    @GetMapping("/{id}")
    public Admission getById(@PathVariable UUID id) {
        return repo.findById(id).orElseThrow(() -> new EntityNotFoundException("Admission not found"));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Admission create(@RequestBody AdmissionRequest req) {
        // The frontend's "New application" form has no applicationNo field, so req.getApplicationNo()
        // was always null — every application's Ref column and its delete-confirm text ("Delete
        // application null?") showed literal "null". Auto-generate like Pupil admission numbers.
        String applicationNo = req.getApplicationNo() == null || req.getApplicationNo().isBlank()
                ? generateApplicationNo() : req.getApplicationNo().trim();
        Admission a = Admission.builder()
                .applicationNo(applicationNo)
                .fullName(req.getFullName())
                .gender(req.getGender())
                .dob(req.getDob())
                .previousSchool(req.getPreviousSchool())
                .parentName(req.getParentName())
                .parentPhone(req.getParentPhone())
                .parentEmail(req.getParentEmail())
                .interviewDate(req.getInterviewDate())
                .regFeePaid(req.getRegFeePaid())
                .regFeePaidOn(req.getRegFeePaid() != null ? (req.getRegFeePaidOn() != null ? req.getRegFeePaidOn() : LocalDate.now()) : null)
                .regFeePaymentMethod(req.getRegFeePaymentMethod())
                .notes(req.getNotes())
                .build();
        if (req.getTargetClassId() != null) classRepo.findById(req.getTargetClassId()).ifPresent(a::setTargetClass);
        return repo.save(a);
    }

    @PutMapping("/{id}")
    public Admission update(@PathVariable UUID id, @RequestBody AdmissionRequest req) {
        Admission a = getById(id);
        a.setFullName(req.getFullName());
        a.setGender(req.getGender());
        a.setDob(req.getDob());
        a.setParentName(req.getParentName());
        a.setParentPhone(req.getParentPhone());
        a.setParentEmail(req.getParentEmail());
        a.setInterviewDate(req.getInterviewDate());
        a.setNotes(req.getNotes());
        if (req.getStatus() != null) a.setStatus(req.getStatus());
        if (req.getTargetClassId() != null) classRepo.findById(req.getTargetClassId()).ifPresent(a::setTargetClass);
        // Recording the registration fee as paid here (amount previously null/zero, now set)
        // is what makes it count as income — stamp today's date if the caller didn't supply one.
        if (req.getRegFeePaid() != null) {
            boolean newlyPaid = a.getRegFeePaid() == null || a.getRegFeePaid().signum() == 0;
            a.setRegFeePaid(req.getRegFeePaid());
            a.setRegFeePaidOn(req.getRegFeePaidOn() != null ? req.getRegFeePaidOn() : (newlyPaid ? LocalDate.now() : a.getRegFeePaidOn()));
        }
        if (req.getRegFeePaymentMethod() != null) a.setRegFeePaymentMethod(req.getRegFeePaymentMethod());
        return repo.save(a);
    }

    @PatchMapping("/{id}/status")
    public Admission updateStatus(@PathVariable UUID id, @RequestParam String status) {
        Admission a = getById(id);
        a.setStatus(AdmissionStatus.valueOf(status.toUpperCase()));
        return repo.save(a);
    }

    // Converts an admitted application into an actual Pupil record — there was previously no
    // way to do this at all, so admins had to separately re-key the same details into Add
    // Pupil. The registration fee (if already collected at application stage) is NOT re-billed
    // here — it was already counted as income the moment it was recorded via update() above.
    @PostMapping("/{id}/enroll")
    @ResponseStatus(HttpStatus.CREATED)
    public Pupil enroll(@PathVariable UUID id, @RequestBody(required = false) EnrollRequest req) {
        Admission a = getById(id);
        if (a.getPupil() != null) throw new IllegalArgumentException("This application has already been enrolled");

        PupilRequest pr = new PupilRequest();
        pr.setFullName(a.getFullName());
        pr.setGender(a.getGender());
        pr.setDob(a.getDob());
        pr.setPreviousSchool(a.getPreviousSchool());
        pr.setClassId(req != null && req.getClassId() != null ? req.getClassId() : (a.getTargetClass() != null ? a.getTargetClass().getId() : null));
        pr.setAdmittedOn(LocalDate.now());
        Pupil pupil = pupilService.create(pr);

        a.setPupil(pupil);
        a.setStatus(AdmissionStatus.ENROLLED);
        repo.save(a);
        return pupil;
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable UUID id) { repo.deleteById(id); }

    private String generateApplicationNo() {
        String candidate;
        do {
            candidate = "ADM-%d-%s".formatted(
                    LocalDate.now().getYear(),
                    UUID.randomUUID().toString().substring(0, 8).toUpperCase(Locale.ROOT));
        } while (repo.existsByApplicationNo(candidate));
        return candidate;
    }

    @Data
    public static class AdmissionRequest {
        private String applicationNo;
        private String fullName;
        private String gender;
        private LocalDate dob;
        private String previousSchool;
        private String parentName;
        private String parentPhone;
        private String parentEmail;
        private LocalDate interviewDate;
        private BigDecimal regFeePaid;
        private LocalDate regFeePaidOn;
        private String regFeePaymentMethod;
        private AdmissionStatus status;
        private UUID targetClassId;
        private String notes;
    }

    @Data
    public static class EnrollRequest {
        private UUID classId;
    }
}

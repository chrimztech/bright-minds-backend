package com.brightminds.school.controller;

import com.brightminds.school.entity.Admission;
import com.brightminds.school.entity.enums.AdmissionStatus;
import com.brightminds.school.repository.AdmissionRepository;
import com.brightminds.school.repository.SchoolClassRepository;
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
import java.util.UUID;

@RestController
@RequestMapping("/admissions")
@RequiredArgsConstructor
@Tag(name = "Admissions")
@PreAuthorize("@perm.has('admissions:manage')")
public class AdmissionController {

    private final AdmissionRepository repo;
    private final SchoolClassRepository classRepo;

    @GetMapping
    public List<Admission> list(@RequestParam(required = false) String status) {
        if (status != null) return repo.findByStatus(AdmissionStatus.valueOf(status.toUpperCase()));
        return repo.findAll();
    }

    @GetMapping("/{id}")
    public Admission getById(@PathVariable UUID id) {
        return repo.findById(id).orElseThrow(() -> new EntityNotFoundException("Admission not found"));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Admission create(@RequestBody AdmissionRequest req) {
        Admission a = Admission.builder()
                .applicationNo(req.getApplicationNo())
                .fullName(req.getFullName())
                .gender(req.getGender())
                .dob(req.getDob())
                .previousSchool(req.getPreviousSchool())
                .parentName(req.getParentName())
                .parentPhone(req.getParentPhone())
                .parentEmail(req.getParentEmail())
                .interviewDate(req.getInterviewDate())
                .regFeePaid(req.getRegFeePaid())
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
        return repo.save(a);
    }

    @PatchMapping("/{id}/status")
    public Admission updateStatus(@PathVariable UUID id, @RequestParam String status) {
        Admission a = getById(id);
        a.setStatus(AdmissionStatus.valueOf(status.toUpperCase()));
        return repo.save(a);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable UUID id) { repo.deleteById(id); }

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
        private AdmissionStatus status;
        private UUID targetClassId;
        private String notes;
    }
}

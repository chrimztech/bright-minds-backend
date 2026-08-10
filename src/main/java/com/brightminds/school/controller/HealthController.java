package com.brightminds.school.controller;

import com.brightminds.school.entity.HealthRecord;
import com.brightminds.school.repository.HealthRecordRepository;
import com.brightminds.school.repository.PupilRepository;
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

@RestController @RequestMapping("/health") @RequiredArgsConstructor @Tag(name = "Health Records")
@PreAuthorize("hasAnyRole('SUPER_ADMIN','ADMIN','HEAD_TEACHER','DEPUTY_HEAD')")
public class HealthController {
    private final HealthRecordRepository repo;
    private final PupilRepository pupilRepo;

    @GetMapping public List<HealthRecord> list(@RequestParam(required = false) UUID pupilId) {
        if (pupilId != null) return repo.findByPupilIdOrderByVisitDateDesc(pupilId);
        return repo.findAll();
    }
    @PostMapping @ResponseStatus(HttpStatus.CREATED) public HealthRecord create(@RequestBody HealthRequest req) {
        var pupil = pupilRepo.findById(req.getPupilId()).orElseThrow(() -> new EntityNotFoundException("Pupil not found"));
        return repo.save(HealthRecord.builder().pupil(pupil).visitDate(req.getVisitDate() != null ? req.getVisitDate() : LocalDate.now())
                .complaint(req.getComplaint()).diagnosis(req.getDiagnosis()).treatment(req.getTreatment())
                .medication(req.getMedication()).notes(req.getNotes()).attendedBy(req.getAttendedBy()).build());
    }
    @PutMapping("/{id}") public HealthRecord update(@PathVariable UUID id, @RequestBody HealthRequest req) {
        var h = repo.findById(id).orElseThrow(() -> new EntityNotFoundException("Record not found"));
        h.setComplaint(req.getComplaint()); h.setDiagnosis(req.getDiagnosis());
        h.setTreatment(req.getTreatment()); h.setMedication(req.getMedication());
        h.setNotes(req.getNotes()); h.setAttendedBy(req.getAttendedBy());
        return repo.save(h);
    }
    @DeleteMapping("/{id}") @ResponseStatus(HttpStatus.NO_CONTENT) public void delete(@PathVariable UUID id) { repo.deleteById(id); }

    @Data public static class HealthRequest {
        private UUID pupilId; private LocalDate visitDate; private String complaint;
        private String diagnosis; private String treatment; private String medication;
        private String notes; private String attendedBy;
    }
}

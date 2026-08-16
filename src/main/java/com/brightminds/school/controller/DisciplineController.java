package com.brightminds.school.controller;

import com.brightminds.school.entity.DisciplineRecord;
import com.brightminds.school.entity.enums.DisciplineKind;
import com.brightminds.school.repository.DisciplineRecordRepository;
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

@RestController @RequestMapping("/discipline") @RequiredArgsConstructor @Tag(name = "Discipline")
@PreAuthorize("@perm.has('discipline:manage')")
public class DisciplineController {
    private final DisciplineRecordRepository repo;
    private final PupilRepository pupilRepo;

    @GetMapping public List<DisciplineRecord> list(@RequestParam(required = false) UUID pupilId) {
        if (pupilId != null) return repo.findByPupilIdOrderByOccurredOnDesc(pupilId);
        return repo.findAll();
    }
    @PostMapping @ResponseStatus(HttpStatus.CREATED) public DisciplineRecord create(@RequestBody DisciplineRequest req) {
        var pupil = pupilRepo.findById(req.getPupilId()).orElseThrow(() -> new EntityNotFoundException("Pupil not found"));
        return repo.save(DisciplineRecord.builder().pupil(pupil)
                .kind(req.getKind() != null ? req.getKind() : DisciplineKind.INCIDENT)
                .description(req.getDescription()).actionTaken(req.getActionTaken())
                .occurredOn(req.getOccurredOn() != null ? req.getOccurredOn() : LocalDate.now())
                .points(req.getPoints()).build());
    }
    @DeleteMapping("/{id}") @ResponseStatus(HttpStatus.NO_CONTENT) public void delete(@PathVariable UUID id) { repo.deleteById(id); }

    @Data public static class DisciplineRequest {
        private UUID pupilId; private DisciplineKind kind; private String description;
        private String actionTaken; private LocalDate occurredOn; private Integer points;
    }
}

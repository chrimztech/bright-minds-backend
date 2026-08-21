package com.brightminds.school.controller;

import com.brightminds.school.entity.DisciplineRecord;
import com.brightminds.school.entity.Pupil;
import com.brightminds.school.entity.enums.DisciplineKind;
import com.brightminds.school.repository.DisciplineRecordRepository;
import com.brightminds.school.repository.PupilRepository;
import com.brightminds.school.service.ClassScopeService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.persistence.EntityNotFoundException;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import java.time.LocalDate;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@RestController @RequestMapping("/discipline") @RequiredArgsConstructor @Tag(name = "Discipline")
public class DisciplineController {
    private final DisciplineRecordRepository repo;
    private final PupilRepository pupilRepo;
    private final ClassScopeService scopeService;

    // discipline:view alone (without :manage) previously granted no access at all — every
    // endpoint including this GET was gated behind :manage, so a role deliberately given
    // view-only rights via Roles & Permissions was silently blocked from viewing anything.
    @PreAuthorize("@perm.has('discipline:view') or @perm.has('discipline:manage')")
    @GetMapping public List<DisciplineRecord> list(@RequestParam(required = false) UUID pupilId, Authentication auth) {
        Set<UUID> scope = scopeService.restrictedClassIds(auth);
        if (pupilId != null) {
            Pupil pupil = pupilRepo.findById(pupilId).orElseThrow(() -> new EntityNotFoundException("Pupil not found"));
            scopeService.assertInScope(scope, pupil.getSchoolClass() != null ? pupil.getSchoolClass().getId() : null);
            return repo.findByPupilIdOrderByOccurredOnDesc(pupilId);
        }
        List<DisciplineRecord> all = repo.findAll();
        if (scope == null) return all;
        return all.stream().filter(d -> d.getPupil().getSchoolClass() != null
                && scope.contains(d.getPupil().getSchoolClass().getId())).toList();
    }
    @PreAuthorize("@perm.has('discipline:manage')")
    @PostMapping @ResponseStatus(HttpStatus.CREATED) public DisciplineRecord create(@RequestBody DisciplineRequest req, Authentication auth) {
        var pupil = pupilRepo.findById(req.getPupilId()).orElseThrow(() -> new EntityNotFoundException("Pupil not found"));
        scopeService.assertInScope(scopeService.restrictedClassIds(auth), pupil.getSchoolClass() != null ? pupil.getSchoolClass().getId() : null);
        return repo.save(DisciplineRecord.builder().pupil(pupil)
                .kind(req.getKind() != null ? req.getKind() : DisciplineKind.INCIDENT)
                .description(req.getDescription()).actionTaken(req.getActionTaken())
                .occurredOn(req.getOccurredOn() != null ? req.getOccurredOn() : LocalDate.now())
                .points(req.getPoints()).build());
    }
    @PreAuthorize("@perm.has('discipline:manage')")
    @DeleteMapping("/{id}") @ResponseStatus(HttpStatus.NO_CONTENT) public void delete(@PathVariable UUID id, Authentication auth) {
        DisciplineRecord d = repo.findById(id).orElseThrow(() -> new EntityNotFoundException("Record not found"));
        scopeService.assertInScope(scopeService.restrictedClassIds(auth), d.getPupil().getSchoolClass() != null ? d.getPupil().getSchoolClass().getId() : null);
        repo.deleteById(id);
    }

    @Data public static class DisciplineRequest {
        private UUID pupilId; private DisciplineKind kind; private String description;
        private String actionTaken; private LocalDate occurredOn; private Integer points;
    }
}

package com.brightminds.school.controller;

import com.brightminds.school.entity.Homework;
import com.brightminds.school.repository.*;
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

@RestController @RequestMapping("/homework") @RequiredArgsConstructor @Tag(name = "Homework")
public class HomeworkController {
    private final HomeworkRepository repo;
    private final SchoolClassRepository classRepo;
    private final SubjectRepository subjectRepo;
    private final ClassScopeService scopeService;

    @GetMapping public List<Homework> list(@RequestParam(required = false) UUID classId, Authentication auth) {
        Set<UUID> scope = scopeService.restrictedClassIds(auth);
        if (classId != null) {
            scopeService.assertInScope(scope, classId);
            return repo.findBySchoolClassIdOrderByDueDateDesc(classId);
        }
        List<Homework> all = repo.findAllByOrderByCreatedAtDesc();
        if (scope == null) return all;
        return all.stream().filter(h -> h.getSchoolClass() != null && scope.contains(h.getSchoolClass().getId())).toList();
    }
    @PreAuthorize("@perm.has('homework:manage')")
    @PostMapping @ResponseStatus(HttpStatus.CREATED) public Homework create(@RequestBody HWRequest req, Authentication auth) {
        scopeService.assertInScope(scopeService.restrictedClassIds(auth), req.getClassId());
        var hw = Homework.builder().title(req.getTitle()).description(req.getDescription())
                .dueDate(req.getDueDate()).attachmentUrl(req.getAttachmentUrl()).build();
        if (req.getClassId() != null) classRepo.findById(req.getClassId()).ifPresent(hw::setSchoolClass);
        if (req.getSubjectId() != null) subjectRepo.findById(req.getSubjectId()).ifPresent(hw::setSubject);
        return repo.save(hw);
    }
    @PreAuthorize("@perm.has('homework:manage')")
    @PutMapping("/{id}") public Homework update(@PathVariable UUID id, @RequestBody HWRequest req, Authentication auth) {
        var hw = repo.findById(id).orElseThrow(() -> new EntityNotFoundException("Homework not found"));
        scopeService.assertInScope(scopeService.restrictedClassIds(auth), hw.getSchoolClass() != null ? hw.getSchoolClass().getId() : null);
        hw.setTitle(req.getTitle()); hw.setDescription(req.getDescription());
        hw.setDueDate(req.getDueDate()); hw.setAttachmentUrl(req.getAttachmentUrl());
        return repo.save(hw);
    }
    @PreAuthorize("@perm.has('homework:manage')")
    @DeleteMapping("/{id}") @ResponseStatus(HttpStatus.NO_CONTENT) public void delete(@PathVariable UUID id, Authentication auth) {
        var hw = repo.findById(id).orElseThrow(() -> new EntityNotFoundException("Homework not found"));
        scopeService.assertInScope(scopeService.restrictedClassIds(auth), hw.getSchoolClass() != null ? hw.getSchoolClass().getId() : null);
        repo.deleteById(id);
    }

    @Data public static class HWRequest {
        private String title; private String description; private UUID classId;
        private UUID subjectId; private LocalDate dueDate; private String attachmentUrl;
    }
}

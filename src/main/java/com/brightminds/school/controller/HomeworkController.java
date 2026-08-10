package com.brightminds.school.controller;

import com.brightminds.school.entity.Homework;
import com.brightminds.school.repository.*;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.persistence.EntityNotFoundException;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@RestController @RequestMapping("/homework") @RequiredArgsConstructor @Tag(name = "Homework")
public class HomeworkController {
    private final HomeworkRepository repo;
    private final SchoolClassRepository classRepo;
    private final SubjectRepository subjectRepo;

    @GetMapping public List<Homework> list(@RequestParam(required = false) UUID classId) {
        if (classId != null) return repo.findBySchoolClassIdOrderByDueDateDesc(classId);
        return repo.findAllByOrderByCreatedAtDesc();
    }
    @PostMapping @ResponseStatus(HttpStatus.CREATED) public Homework create(@RequestBody HWRequest req) {
        var hw = Homework.builder().title(req.getTitle()).description(req.getDescription())
                .dueDate(req.getDueDate()).attachmentUrl(req.getAttachmentUrl()).build();
        if (req.getClassId() != null) classRepo.findById(req.getClassId()).ifPresent(hw::setSchoolClass);
        if (req.getSubjectId() != null) subjectRepo.findById(req.getSubjectId()).ifPresent(hw::setSubject);
        return repo.save(hw);
    }
    @PutMapping("/{id}") public Homework update(@PathVariable UUID id, @RequestBody HWRequest req) {
        var hw = repo.findById(id).orElseThrow(() -> new EntityNotFoundException("Homework not found"));
        hw.setTitle(req.getTitle()); hw.setDescription(req.getDescription());
        hw.setDueDate(req.getDueDate()); hw.setAttachmentUrl(req.getAttachmentUrl());
        return repo.save(hw);
    }
    @DeleteMapping("/{id}") @ResponseStatus(HttpStatus.NO_CONTENT) public void delete(@PathVariable UUID id) { repo.deleteById(id); }

    @Data public static class HWRequest {
        private String title; private String description; private UUID classId;
        private UUID subjectId; private LocalDate dueDate; private String attachmentUrl;
    }
}

package com.brightminds.school.controller;

import com.brightminds.school.entity.Subject;
import com.brightminds.school.repository.SubjectRepository;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.persistence.EntityNotFoundException;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.UUID;

@RestController @RequestMapping("/subjects") @RequiredArgsConstructor @Tag(name = "Subjects")
public class SubjectController {
    private final SubjectRepository repo;
    // GETs left open to all authenticated users: subjects are referenced as dropdown data
    // by exams, marks, homework and timetable pages across teaching/admin roles.
    @GetMapping public List<Subject> list() { return repo.findAll(); }
    @GetMapping("/{id}") public Subject get(@PathVariable UUID id) { return repo.findById(id).orElseThrow(() -> new EntityNotFoundException("Not found")); }
    @PreAuthorize("@perm.has('subjects:manage')")
    @PostMapping @ResponseStatus(HttpStatus.CREATED) public Subject create(@RequestBody SubjectReq req) {
        return repo.save(Subject.builder().name(req.getName()).code(req.getCode()).build()); }
    @PreAuthorize("@perm.has('subjects:manage')")
    @PutMapping("/{id}") public Subject update(@PathVariable UUID id, @RequestBody SubjectReq req) {
        Subject s = get(id); s.setName(req.getName()); s.setCode(req.getCode()); return repo.save(s); }
    @PreAuthorize("@perm.has('subjects:manage')")
    @DeleteMapping("/{id}") @ResponseStatus(HttpStatus.NO_CONTENT) public void delete(@PathVariable UUID id) { repo.deleteById(id); }
    @Data public static class SubjectReq { private String name; private String code; }
}

package com.brightminds.school.controller;

import com.brightminds.school.dto.PageResponse;
import com.brightminds.school.dto.PupilRequest;
import com.brightminds.school.entity.Pupil;
import com.brightminds.school.service.PupilService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/pupils")
@RequiredArgsConstructor
@Tag(name = "Pupils")
@PreAuthorize("hasAnyRole('SUPER_ADMIN','ADMIN','HEAD_TEACHER','DEPUTY_HEAD','TEACHER','CLASS_TEACHER')")
public class PupilController {

    private final PupilService pupilService;

    @GetMapping
    @Operation(summary = "Get all pupils or search")
    public PageResponse<Pupil> list(
            @RequestParam(required = false) String q,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size) {
        return pupilService.search(q, page, size);
    }

    @GetMapping("/all")
    public List<Pupil> all() {
        return pupilService.getAll();
    }

    @GetMapping("/{id}")
    public Pupil getById(@PathVariable UUID id) {
        return pupilService.getById(id);
    }

    @GetMapping("/class/{classId}")
    public List<Pupil> getByClass(@PathVariable UUID classId) {
        return pupilService.getByClass(classId);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Pupil create(@Valid @RequestBody PupilRequest req) {
        return pupilService.create(req);
    }

    @PutMapping("/{id}")
    public Pupil update(@PathVariable UUID id, @Valid @RequestBody PupilRequest req) {
        return pupilService.update(id, req);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable UUID id) {
        pupilService.delete(id);
    }
}

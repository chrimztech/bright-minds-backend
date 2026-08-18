package com.brightminds.school.controller;

import com.brightminds.school.dto.PageResponse;
import com.brightminds.school.dto.PupilRequest;
import com.brightminds.school.entity.Pupil;
import com.brightminds.school.service.ClassScopeService;
import com.brightminds.school.service.PupilService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Set;
import java.util.UUID;

@RestController
@RequestMapping("/pupils")
@RequiredArgsConstructor
@Tag(name = "Pupils")
@PreAuthorize("@perm.has('pupils:view')")
public class PupilController {

    private final PupilService pupilService;
    private final ClassScopeService scopeService;

    @GetMapping
    @Operation(summary = "Get all pupils or search")
    public PageResponse<Pupil> list(
            @RequestParam(required = false) String q,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size,
            Authentication auth) {
        PageResponse<Pupil> result = pupilService.search(q, page, size);
        Set<UUID> scope = scopeService.restrictedClassIds(auth);
        if (scope == null) return result;
        List<Pupil> filtered = result.getContent().stream()
                .filter(p -> p.getSchoolClass() != null && scope.contains(p.getSchoolClass().getId())).toList();
        return PageResponse.<Pupil>builder()
                .content(filtered).page(result.getPage()).size(result.getSize())
                .totalElements(filtered.size()).totalPages(1).build();
    }

    @GetMapping("/all")
    public List<Pupil> all(Authentication auth) {
        List<Pupil> all = pupilService.getAll();
        Set<UUID> scope = scopeService.restrictedClassIds(auth);
        if (scope == null) return all;
        return all.stream().filter(p -> p.getSchoolClass() != null && scope.contains(p.getSchoolClass().getId())).toList();
    }

    @GetMapping("/{id}")
    public Pupil getById(@PathVariable UUID id, Authentication auth) {
        Pupil p = pupilService.getById(id);
        Set<UUID> scope = scopeService.restrictedClassIds(auth);
        if (scope != null && (p.getSchoolClass() == null || !scope.contains(p.getSchoolClass().getId()))) {
            throw new AccessDeniedException("You're only permitted to view pupils in classes you teach.");
        }
        return p;
    }

    @GetMapping("/class/{classId}")
    public List<Pupil> getByClass(@PathVariable UUID classId, Authentication auth) {
        scopeService.assertInScope(scopeService.restrictedClassIds(auth), classId);
        return pupilService.getByClass(classId);
    }

    @PreAuthorize("@perm.has('pupils:create')")
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Pupil create(@Valid @RequestBody PupilRequest req, Authentication auth) {
        scopeService.assertInScope(scopeService.restrictedClassIds(auth), req.getClassId());
        return pupilService.create(req);
    }

    @PreAuthorize("@perm.has('pupils:edit')")
    @PutMapping("/{id}")
    public Pupil update(@PathVariable UUID id, @Valid @RequestBody PupilRequest req, Authentication auth) {
        Set<UUID> scope = scopeService.restrictedClassIds(auth);
        if (scope != null) {
            Pupil existing = pupilService.getById(id);
            scopeService.assertInScope(scope, existing.getSchoolClass() != null ? existing.getSchoolClass().getId() : null);
            scopeService.assertInScope(scope, req.getClassId());
        }
        return pupilService.update(id, req);
    }

    @PreAuthorize("@perm.has('pupils:delete')")
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable UUID id, Authentication auth) {
        Set<UUID> scope = scopeService.restrictedClassIds(auth);
        if (scope != null) {
            Pupil existing = pupilService.getById(id);
            scopeService.assertInScope(scope, existing.getSchoolClass() != null ? existing.getSchoolClass().getId() : null);
        }
        pupilService.delete(id);
    }
}

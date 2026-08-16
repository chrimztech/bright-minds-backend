package com.brightminds.school.controller;

import com.brightminds.school.entity.AppUser;
import com.brightminds.school.entity.SchoolClass;
import com.brightminds.school.entity.Staff;
import com.brightminds.school.repository.AppUserRepository;
import com.brightminds.school.repository.SchoolClassRepository;
import com.brightminds.school.repository.StaffRepository;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.persistence.EntityNotFoundException;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/classes")
@RequiredArgsConstructor
@Tag(name = "Classes")
@PreAuthorize("@perm.has('classes:view')")
public class ClassController {

    private final SchoolClassRepository classRepo;
    private final StaffRepository staffRepo;
    private final AppUserRepository userRepo;

    @GetMapping
    public List<SchoolClass> list() {
        return classRepo.findAllByOrderByLevelOrderAsc();
    }

    @GetMapping("/my")
    public List<SchoolClass> myClasses(@AuthenticationPrincipal UserDetails principal) {
        AppUser user = userRepo.findByEmail(principal.getUsername()).orElse(null);
        if (user == null) return List.of();
        return classRepo.findByClassTeacherUserId(user.getId());
    }

    @GetMapping("/{id}")
    public SchoolClass getById(@PathVariable UUID id) {
        return classRepo.findById(id).orElseThrow(() -> new EntityNotFoundException("Class not found"));
    }

    @PreAuthorize("@perm.has('classes:manage')")
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public SchoolClass create(@Valid @RequestBody ClassRequest req) {
        SchoolClass sc = SchoolClass.builder()
                .name(req.getName())
                .stream(req.getStream())
                .levelOrder(req.getLevelOrder() != null ? req.getLevelOrder() : 1)
                .capacity(req.getCapacity())
                .build();
        if (req.getClassTeacherId() != null) {
            staffRepo.findById(req.getClassTeacherId()).ifPresent(sc::setClassTeacher);
        }
        return classRepo.save(sc);
    }

    // Not @Valid: the roster page's quick "Assign teacher" dropdown sends only
    // classTeacherId, not the full class record, so every other field here is a
    // partial-merge — applied only when present, never forced back to a default.
    @PreAuthorize("@perm.has('classes:manage')")
    @PutMapping("/{id}")
    public SchoolClass update(@PathVariable UUID id, @RequestBody ClassRequest req) {
        SchoolClass sc = classRepo.findById(id).orElseThrow(() -> new EntityNotFoundException("Class not found"));
        if (req.getName() != null) sc.setName(req.getName());
        if (req.getStream() != null) sc.setStream(req.getStream());
        if (req.getLevelOrder() != null) sc.setLevelOrder(req.getLevelOrder());
        if (req.getCapacity() != null) sc.setCapacity(req.getCapacity());
        // Both callers (the full class-edit form and the roster's quick reassign dropdown)
        // always send this key explicitly — a real id to set it, or null to clear it — so
        // unlike the fields above, there's no "omitted" case to preserve here.
        sc.setClassTeacher(req.getClassTeacherId() != null
                ? staffRepo.findById(req.getClassTeacherId()).orElse(null)
                : null);
        return classRepo.save(sc);
    }

    @PreAuthorize("@perm.has('classes:manage')")
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable UUID id) {
        classRepo.deleteById(id);
    }

    @Data
    public static class ClassRequest {
        @NotBlank
        private String name;
        private String stream;
        // Boxed (not a primitive with a default) so a partial update payload that omits
        // this field is distinguishable from one that explicitly sets it — see update().
        private Integer levelOrder;
        private Integer capacity;
        private UUID classTeacherId;
    }
}

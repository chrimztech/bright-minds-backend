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
@PreAuthorize("hasAnyRole('SUPER_ADMIN','ADMIN','HEAD_TEACHER','DEPUTY_HEAD','TEACHER','CLASS_TEACHER')")
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

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public SchoolClass create(@Valid @RequestBody ClassRequest req) {
        SchoolClass sc = SchoolClass.builder()
                .name(req.getName())
                .stream(req.getStream())
                .levelOrder(req.getLevelOrder())
                .capacity(req.getCapacity())
                .build();
        if (req.getClassTeacherId() != null) {
            staffRepo.findById(req.getClassTeacherId()).ifPresent(sc::setClassTeacher);
        }
        return classRepo.save(sc);
    }

    @PutMapping("/{id}")
    public SchoolClass update(@PathVariable UUID id, @Valid @RequestBody ClassRequest req) {
        SchoolClass sc = classRepo.findById(id).orElseThrow(() -> new EntityNotFoundException("Class not found"));
        sc.setName(req.getName());
        sc.setStream(req.getStream());
        sc.setLevelOrder(req.getLevelOrder());
        sc.setCapacity(req.getCapacity());
        if (req.getClassTeacherId() != null) {
            staffRepo.findById(req.getClassTeacherId()).ifPresent(sc::setClassTeacher);
        }
        return classRepo.save(sc);
    }

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
        private int levelOrder = 1;
        private Integer capacity;
        private UUID classTeacherId;
    }
}

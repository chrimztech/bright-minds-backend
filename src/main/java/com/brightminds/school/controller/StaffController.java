package com.brightminds.school.controller;

import com.brightminds.school.dto.PageResponse;
import com.brightminds.school.dto.StaffRequest;
import com.brightminds.school.entity.AppUser;
import com.brightminds.school.entity.Staff;
import com.brightminds.school.repository.AppUserRepository;
import com.brightminds.school.repository.StaffRepository;
import com.brightminds.school.service.StaffService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/staff")
@RequiredArgsConstructor
@Tag(name = "Staff")
@PreAuthorize("hasAnyRole('SUPER_ADMIN','ADMIN','HEAD_TEACHER','DEPUTY_HEAD','TEACHER','CLASS_TEACHER')")
public class StaffController {

    private final StaffService staffService;
    private final StaffRepository staffRepo;
    private final AppUserRepository userRepo;

    @GetMapping
    public PageResponse<Staff> list(
            @RequestParam(required = false) String q,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size) {
        return staffService.search(q, page, size);
    }

    @GetMapping("/all")
    public List<Staff> all() { return staffService.getAll(); }

    @GetMapping("/teachers")
    public List<Staff> teachers() { return staffService.getTeachers(); }

    @GetMapping("/me")
    public Staff me(@AuthenticationPrincipal UserDetails principal) {
        AppUser user = userRepo.findByEmail(principal.getUsername()).orElse(null);
        if (user == null) return null;
        return staffRepo.findByUserId(user.getId()).orElse(null);
    }

    @GetMapping("/{id}")
    public Staff getById(@PathVariable UUID id) { return staffService.getById(id); }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Staff create(@Valid @RequestBody StaffRequest req) { return staffService.create(req); }

    @PutMapping("/{id}")
    public Staff update(@PathVariable UUID id, @Valid @RequestBody StaffRequest req) {
        return staffService.update(id, req);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable UUID id) { staffService.delete(id); }
}

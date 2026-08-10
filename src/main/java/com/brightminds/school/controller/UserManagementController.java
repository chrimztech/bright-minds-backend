package com.brightminds.school.controller;

import com.brightminds.school.entity.AppUser;
import com.brightminds.school.entity.UserRole;
import com.brightminds.school.entity.enums.AppRole;
import com.brightminds.school.repository.AppUserRepository;
import com.brightminds.school.service.AuditService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.persistence.EntityNotFoundException;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/admin/users")
@RequiredArgsConstructor
@Tag(name = "User Management")
@PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN', 'HEAD_TEACHER')")
public class UserManagementController {

    private final AppUserRepository userRepo;
    private final PasswordEncoder passwordEncoder;
    private final AuditService audit;

    @GetMapping
    public List<UserDto> list() {
        return userRepo.findAll().stream().map(this::toDto).collect(Collectors.toList());
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public UserDto create(@RequestBody CreateUserReq req) {
        if (userRepo.existsByEmail(req.getEmail())) {
            throw new IllegalArgumentException("Email already in use");
        }
        AppUser user = AppUser.builder()
                .email(req.getEmail())
                .passwordHash(passwordEncoder.encode(req.getPassword()))
                .fullName(req.getFullName())
                .mustChangePassword(true)
                .build();
        List<AppRole> roles = req.getRoles() != null
                ? req.getRoles().stream().map(AppRole::valueOf).collect(Collectors.toList())
                : List.of(AppRole.TEACHER);
        List<UserRole> userRoles = roles.stream()
                .map(r -> UserRole.builder().user(user).role(r).build())
                .collect(Collectors.toList());
        user.setRoles(userRoles);
        AppUser saved = userRepo.save(user);
        audit.log("CREATE_USER", "AppUser", saved.getId().toString(), saved.getEmail());
        return toDto(saved);
    }

    @PutMapping("/{id}/roles")
    public UserDto updateRoles(@PathVariable UUID id, @RequestBody RolesReq req) {
        AppUser user = userRepo.findById(id).orElseThrow(() -> new EntityNotFoundException("User not found"));
        user.getRoles().clear();
        if (req.getRoles() != null) {
            req.getRoles().forEach(r -> user.getRoles().add(UserRole.builder().user(user).role(AppRole.valueOf(r)).build()));
        }
        UserDto dto = toDto(userRepo.save(user));
        audit.log("UPDATE_ROLES", "AppUser", id.toString(), null);
        return dto;
    }

    @PostMapping("/{id}/reset-password")
    public void resetPassword(@PathVariable UUID id, @RequestBody ResetPasswordReq req) {
        AppUser user = userRepo.findById(id).orElseThrow(() -> new EntityNotFoundException("User not found"));
        user.setPasswordHash(passwordEncoder.encode(req.getPassword()));
        userRepo.save(user);
        audit.log("RESET_PASSWORD", "AppUser", id.toString(), null);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable UUID id) {
        userRepo.deleteById(id);
        audit.log("DELETE_USER", "AppUser", id.toString(), null);
    }

    private UserDto toDto(AppUser u) {
        UserDto dto = new UserDto();
        dto.setId(u.getId().toString());
        dto.setEmail(u.getEmail());
        dto.setFullName(u.getFullName());
        dto.setCreatedAt(u.getCreatedAt() != null ? u.getCreatedAt().toString() : null);
        dto.setRoles(u.getRoles().stream().map(r -> r.getRole().name()).collect(Collectors.toList()));
        dto.setMustChangePassword(u.isMustChangePassword());
        return dto;
    }

    @Data public static class UserDto {
        private String id, email, fullName, createdAt;
        private List<String> roles;
        private boolean mustChangePassword;
    }

    @Data public static class CreateUserReq {
        private String email, password, fullName;
        private List<String> roles;
    }

    @Data public static class RolesReq {
        private List<String> roles;
    }

    @Data public static class ResetPasswordReq {
        private String password;
    }
}

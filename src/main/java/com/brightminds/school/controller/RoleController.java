package com.brightminds.school.controller;

import com.brightminds.school.entity.Permission;
import com.brightminds.school.entity.Role;
import com.brightminds.school.repository.PermissionRepository;
import com.brightminds.school.repository.RoleRepository;
import com.brightminds.school.service.AuditService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.persistence.EntityNotFoundException;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/admin/roles")
@RequiredArgsConstructor
@Tag(name = "Roles & Permissions")
@PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN')")
public class RoleController {

    private final RoleRepository roleRepo;
    private final PermissionRepository permRepo;
    private final AuditService audit;

    @GetMapping
    public List<Role> listRoles() { return roleRepo.findAll(); }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Role createRole(@RequestBody RoleRequest req) {
        if (roleRepo.existsByName(req.getName().toUpperCase())) {
            throw new IllegalArgumentException("Role already exists: " + req.getName());
        }
        Role role = Role.builder()
                .name(req.getName().toUpperCase())
                .description(req.getDescription())
                .isSystem(false)
                .build();
        Role saved = roleRepo.save(role);
        audit.log("CREATE_ROLE", "Role", saved.getId().toString(), saved.getName());
        return saved;
    }

    @PutMapping("/{id}")
    public Role updateRole(@PathVariable UUID id, @RequestBody RoleRequest req) {
        Role role = roleRepo.findById(id).orElseThrow(() -> new EntityNotFoundException("Role not found"));
        if (role.isSystem()) throw new IllegalArgumentException("Cannot edit system roles");
        role.setDescription(req.getDescription());
        return roleRepo.save(role);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteRole(@PathVariable UUID id) {
        Role role = roleRepo.findById(id).orElseThrow(() -> new EntityNotFoundException("Role not found"));
        if (role.isSystem()) throw new IllegalArgumentException("Cannot delete system roles");
        roleRepo.deleteById(id);
        audit.log("DELETE_ROLE", "Role", id.toString(), role.getName());
    }

    @GetMapping("/permissions")
    public List<Permission> listPermissions() { return permRepo.findAll(); }

    @PostMapping("/permissions")
    @ResponseStatus(HttpStatus.CREATED)
    public Permission createPermission(@RequestBody PermReq req) {
        return permRepo.save(Permission.builder()
                .name(req.getName())
                .description(req.getDescription())
                .module(req.getModule() != null ? req.getModule() : "GENERAL")
                .build());
    }

    @DeleteMapping("/permissions/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deletePermission(@PathVariable UUID id) { permRepo.deleteById(id); }

    @PutMapping("/{id}/permissions")
    public Role setRolePermissions(@PathVariable UUID id, @RequestBody List<UUID> permissionIds) {
        Role role = roleRepo.findById(id).orElseThrow(() -> new EntityNotFoundException("Role not found"));
        List<Permission> perms = permRepo.findAllById(permissionIds);
        role.setPermissions(perms);
        return roleRepo.save(role);
    }

    @Data public static class RoleRequest {
        private String name;
        private String description;
    }

    @Data public static class PermReq {
        private String name;
        private String description;
        private String module;
    }
}

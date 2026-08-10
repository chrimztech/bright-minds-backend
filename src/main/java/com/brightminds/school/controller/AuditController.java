package com.brightminds.school.controller;

import com.brightminds.school.entity.AuditLog;
import com.brightminds.school.repository.AuditLogRepository;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/admin/audit")
@RequiredArgsConstructor
@Tag(name = "Audit Logs")
@PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN', 'HEAD_TEACHER')")
public class AuditController {

    private final AuditLogRepository auditRepo;

    @GetMapping
    public Page<AuditLog> list(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size,
            @RequestParam(required = false) String entity,
            @RequestParam(required = false) UUID userId) {
        PageRequest pr = PageRequest.of(page, size);
        if (entity != null && !entity.isBlank()) return auditRepo.findByEntityOrderByCreatedAtDesc(entity, pr);
        if (userId != null) return auditRepo.findByUserIdOrderByCreatedAtDesc(userId, pr);
        return auditRepo.findAllByOrderByCreatedAtDesc(pr);
    }
}

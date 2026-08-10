package com.brightminds.school.service;

import com.brightminds.school.entity.AuditLog;
import com.brightminds.school.repository.AuditLogRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import com.brightminds.school.repository.AppUserRepository;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AuditService {

    private final AuditLogRepository auditRepo;
    private final AppUserRepository userRepo;

    public void log(String action, String entity, String entityId, String details) {
        UUID userId = null;
        try {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            if (auth != null && auth.isAuthenticated() && !"anonymousUser".equals(auth.getPrincipal())) {
                String email = auth.getName();
                userId = userRepo.findByEmail(email).map(u -> u.getId()).orElse(null);
            }
        } catch (Exception ignored) {}

        auditRepo.save(AuditLog.builder()
                .action(action)
                .entity(entity)
                .entityId(entityId)
                .details(details)
                .userId(userId)
                .build());
    }

    public void log(String action, String entity) {
        log(action, entity, null, null);
    }
}

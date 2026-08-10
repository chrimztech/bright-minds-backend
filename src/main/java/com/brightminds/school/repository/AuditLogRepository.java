package com.brightminds.school.repository;

import com.brightminds.school.entity.AuditLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.UUID;

public interface AuditLogRepository extends JpaRepository<AuditLog, UUID> {
    Page<AuditLog> findAllByOrderByCreatedAtDesc(Pageable pageable);
    Page<AuditLog> findByEntityOrderByCreatedAtDesc(String entity, Pageable pageable);
    Page<AuditLog> findByUserIdOrderByCreatedAtDesc(UUID userId, Pageable pageable);
}

package com.brightminds.school.repository;

import com.brightminds.school.entity.Invoice;
import com.brightminds.school.entity.enums.InvoiceStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface InvoiceRepository extends JpaRepository<Invoice, UUID> {
    List<Invoice> findAllByOrderByCreatedAtDesc();
    List<Invoice> findByPupilIdInOrderByCreatedAtDesc(List<UUID> pupilIds);
    List<Invoice> findByPupilIdOrderByCreatedAtDesc(UUID pupilId);
    List<Invoice> findByTerm_IdOrderByCreatedAtDesc(UUID termId);
    List<Invoice> findByStatus(InvoiceStatus status);
    Optional<Invoice> findByInvoiceNo(String invoiceNo);
    boolean existsByInvoiceNo(String invoiceNo);
}

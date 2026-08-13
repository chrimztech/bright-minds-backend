package com.brightminds.school.repository;

import com.brightminds.school.entity.Payment;
import com.brightminds.school.entity.enums.PaymentStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public interface PaymentRepository extends JpaRepository<Payment, UUID> {
    List<Payment> findAllByOrderByPaidOnDesc();
    List<Payment> findByPupilIdOrderByPaidOnDesc(UUID pupilId);
    List<Payment> findByInvoiceIdOrderByPaidOnDesc(UUID invoiceId);
    List<Payment> findByStatusOrderByCreatedAtDesc(PaymentStatus status);

    @Query("SELECT COALESCE(SUM(p.amount),0) FROM Payment p WHERE p.paidOn >= :from AND p.status = 'CONFIRMED'")
    BigDecimal sumAmountFrom(@Param("from") LocalDate from);
}

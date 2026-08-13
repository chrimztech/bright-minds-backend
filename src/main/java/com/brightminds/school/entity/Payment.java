package com.brightminds.school.entity;

import com.brightminds.school.entity.enums.PaymentMethod;
import com.brightminds.school.entity.enums.PaymentStatus;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "payments")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Payment {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "receipt_no", nullable = false, unique = true)
    private String receiptNo;

    // Eager: this entity is always serialized directly to JSON (payment lists, receipts,
    // pending-confirmation queues) with these nested — lazy proxies aren't Jackson-serializable
    // without an extra Hibernate module, so this avoided a 500 on every payments list fetch.
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "pupil_id", nullable = false)
    private Pupil pupil;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "invoice_id")
    private Invoice invoice;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal amount;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Builder.Default
    private PaymentMethod method = PaymentMethod.CASH;

    @Column(name = "paid_on")
    @Builder.Default
    private LocalDate paidOn = LocalDate.now();

    private String reference;
    private String notes;

    @Column(name = "recorded_by")
    private UUID recordedBy;

    @Enumerated(EnumType.STRING)
    @Builder.Default
    private PaymentStatus status = PaymentStatus.CONFIRMED;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "submitted_by_guardian_id")
    private Guardian submittedBy;

    @Column(name = "rejection_reason")
    private String rejectionReason;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private Instant createdAt;
}

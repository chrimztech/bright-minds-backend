package com.brightminds.school.entity;

import com.brightminds.school.entity.enums.GatewayTransactionStatus;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "gateway_transactions")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class GatewayTransaction {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Builder.Default
    private String provider = "LENCO";

    @Column(nullable = false, unique = true)
    private String reference;

    @Column(name = "lenco_id")
    private String lencoId;

    @Column(name = "lenco_reference")
    private String lencoReference;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "invoice_id", nullable = false)
    private Invoice invoice;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "guardian_id")
    private Guardian guardian;

    @Column(nullable = false)
    private String phone;

    @Column(nullable = false)
    private String operator;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal amount;

    @Enumerated(EnumType.STRING)
    @Builder.Default
    private GatewayTransactionStatus status = GatewayTransactionStatus.PENDING;

    @Column(name = "payment_id")
    private UUID paymentId;

    @Column(name = "failure_reason", columnDefinition = "TEXT")
    private String failureReason;

    @Column(name = "raw_response", columnDefinition = "TEXT")
    private String rawResponse;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private Instant updatedAt;
}

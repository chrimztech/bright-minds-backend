package com.brightminds.school.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "canteen_sales")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class CanteenSale {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "item_id")
    private CanteenMenuItem item;

    @Column(name = "item_name", nullable = false)
    private String itemName;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "pupil_id")
    private Pupil pupil;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "staff_id")
    private Staff staff;

    @Builder.Default
    private int quantity = 1;

    @Column(name = "unit_price", precision = 10, scale = 2)
    @Builder.Default
    private BigDecimal unitPrice = BigDecimal.ZERO;

    @Column(precision = 10, scale = 2)
    @Builder.Default
    private BigDecimal total = BigDecimal.ZERO;

    @Column(name = "payment_method")
    @Builder.Default
    private String paymentMethod = "cash";

    @Column(name = "served_on")
    @Builder.Default
    private LocalDate servedOn = LocalDate.now();

    private String notes;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private Instant createdAt;
}

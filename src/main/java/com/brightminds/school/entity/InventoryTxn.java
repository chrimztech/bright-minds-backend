package com.brightminds.school.entity;

import com.brightminds.school.entity.enums.InvDirection;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "inventory_txns")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class InventoryTxn {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "item_id", nullable = false)
    private InventoryItem item;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Column(nullable = false)
    private InvDirection direction;

    @Column(nullable = false)
    private int quantity;

    private String reason;
    private String reference;

    @Column(name = "occurred_on")
    @Builder.Default
    private LocalDate occurredOn = LocalDate.now();

    @Column(name = "created_by")
    private UUID createdBy;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private Instant createdAt;
}

package com.brightminds.school.entity;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "fee_items")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class FeeItem {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    @Builder.Default
    private String category = "SCHOOL_FEE";

    @Column(nullable = false, precision = 12, scale = 2)
    @Builder.Default
    private BigDecimal amount = BigDecimal.ZERO;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "class_id")
    private SchoolClass schoolClass;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "term_id")
    private Term term;

    @Column(name = "is_recurring")
    @Builder.Default
    private boolean isRecurring = true;

    // Carried onto every invoice auto-billed from this item (see FeeAutoBillingService) so
    // the existing late-fee sweep, which keys off Invoice.dueDate, applies here too — mainly
    // relevant for one-off charges like an Administrative or Registration fee.
    @Column(name = "due_date")
    private LocalDate dueDate;

    // Explicit accessor: Jackson strips the "is" prefix from Lombok's isRecurring()
    // by default, which would serialize this as "recurring" instead of "isRecurring".
    @JsonProperty("isRecurring")
    public boolean isRecurring() { return isRecurring; }

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private Instant createdAt;
}

package com.brightminds.school.entity;

import com.brightminds.school.entity.enums.LoanStatus;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "library_loans")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class LibraryLoan {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "book_id", nullable = false)
    private LibraryBook book;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "pupil_id")
    private Pupil pupil;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "staff_id")
    private Staff staff;

    @Column(name = "borrowed_on")
    @Builder.Default
    private LocalDate borrowedOn = LocalDate.now();

    @Column(name = "due_on", nullable = false)
    private LocalDate dueOn;

    @Column(name = "returned_on")
    private LocalDate returnedOn;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Builder.Default
    private LoanStatus status = LoanStatus.BORROWED;

    @Column(precision = 10, scale = 2)
    private BigDecimal fine;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private Instant updatedAt;
}

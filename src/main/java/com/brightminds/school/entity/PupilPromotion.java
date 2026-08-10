package com.brightminds.school.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "pupil_promotions")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class PupilPromotion {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "pupil_id", nullable = false)
    private Pupil pupil;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "from_class_id", nullable = false)
    private SchoolClass fromClass;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "to_class_id", nullable = false)
    private SchoolClass toClass;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "academic_year_id")
    private AcademicYear academicYear;

    @Column(name = "promoted_on", nullable = false)
    private LocalDate promotedOn;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "promoted_by")
    private AppUser promotedBy;

    private String notes;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private Instant createdAt;
}

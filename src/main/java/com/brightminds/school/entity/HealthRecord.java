package com.brightminds.school.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "health_records")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class HealthRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "pupil_id", nullable = false)
    private Pupil pupil;

    @Column(name = "visit_date")
    @Builder.Default
    private LocalDate visitDate = LocalDate.now();

    private String complaint;
    private String diagnosis;
    private String treatment;
    private String medication;

    @Column(columnDefinition = "TEXT")
    private String notes;

    @Column(name = "attended_by")
    private String attendedBy;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private Instant updatedAt;
}

package com.brightminds.school.entity;

import com.brightminds.school.entity.enums.DisciplineKind;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "discipline_records")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class DisciplineRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "pupil_id", nullable = false)
    private Pupil pupil;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Builder.Default
    private DisciplineKind kind = DisciplineKind.INCIDENT;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String description;

    @Column(name = "action_taken", columnDefinition = "TEXT")
    private String actionTaken;

    @Column(name = "occurred_on")
    @Builder.Default
    private LocalDate occurredOn = LocalDate.now();

    private Integer points;

    @Column(name = "reported_by")
    private UUID reportedBy;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private Instant updatedAt;
}

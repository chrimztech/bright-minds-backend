package com.brightminds.school.entity;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "academic_years")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class AcademicYear {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private String name;

    @Column(name = "start_date", nullable = false)
    private LocalDate startDate;

    @Column(name = "end_date", nullable = false)
    private LocalDate endDate;

    @Column(name = "is_current")
    @Builder.Default
    private boolean isCurrent = false;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private Instant createdAt;

    // Explicit accessor: Jackson strips the "is" prefix from Lombok's isCurrent()
    // by default, which would serialize this as "current" instead of "isCurrent".
    @JsonProperty("isCurrent")
    public boolean isCurrent() { return isCurrent; }
}

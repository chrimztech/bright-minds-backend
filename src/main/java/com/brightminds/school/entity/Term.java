package com.brightminds.school.entity;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "terms")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Term {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "academic_year_id", nullable = false)
    private AcademicYear academicYear;

    @Column(nullable = false)
    private String name;

    @Column(name = "start_date", nullable = false)
    private LocalDate startDate;

    @Column(name = "end_date", nullable = false)
    private LocalDate endDate;

    @Column(name = "is_current")
    @Builder.Default
    private boolean isCurrent = false;

    // Explicit accessor: Jackson strips the "is" prefix from Lombok's isCurrent()
    // by default, which would serialize this as "current" instead of "isCurrent".
    @JsonProperty("isCurrent")
    public boolean isCurrent() { return isCurrent; }
}

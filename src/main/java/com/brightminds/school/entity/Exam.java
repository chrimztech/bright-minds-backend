package com.brightminds.school.entity;

import com.brightminds.school.entity.enums.AssessmentType;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "exams")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Exam {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private String name;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "term_id")
    private Term term;

    @Enumerated(EnumType.STRING)
    @Column(name = "assessment_type")
    private AssessmentType assessmentType;

    @Column(name = "exam_date")
    private LocalDate examDate;

    @Column(name = "out_of")
    @Builder.Default
    private int outOf = 100;

    @Builder.Default
    private double weight = 1.0;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private Instant createdAt;

    @JsonProperty("termId")
    public UUID getTermId() {
        return term != null ? term.getId() : null;
    }
}

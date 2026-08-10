package com.brightminds.school.entity;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.persistence.*;
import lombok.*;

import java.util.UUID;

@Entity
@Table(name = "guardian_pupils")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class GuardianPupil {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "guardian_id", nullable = false)
    private Guardian guardian;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "pupil_id", nullable = false)
    private Pupil pupil;

    @Column(name = "is_primary")
    @Builder.Default
    private boolean isPrimary = false;

    // Explicit accessor: Jackson strips the "is" prefix from Lombok's isPrimary()
    // by default, which would serialize this as "primary" instead of "isPrimary".
    @JsonProperty("isPrimary")
    public boolean isPrimary() { return isPrimary; }
}

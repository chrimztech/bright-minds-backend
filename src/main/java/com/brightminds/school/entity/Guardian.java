package com.brightminds.school.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "guardians")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Guardian {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "full_name", nullable = false)
    private String fullName;

    private String relationship;
    private String phone;
    private String email;
    private String address;
    private String occupation;

    @Column(name = "national_id")
    private String nationalId;

    @Column(name = "user_id")
    private UUID userId;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private Instant updatedAt;
}

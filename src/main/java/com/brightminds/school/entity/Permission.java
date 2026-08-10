package com.brightminds.school.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "permissions")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Permission {
    @Id @GeneratedValue(strategy = GenerationType.UUID) private UUID id;
    @Column(nullable = false, unique = true) private String name;
    private String description;
    @Column(nullable = false) @Builder.Default private String module = "GENERAL";
    @CreationTimestamp @Column(name = "created_at", updatable = false) private Instant createdAt;
}

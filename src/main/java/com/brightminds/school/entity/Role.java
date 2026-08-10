package com.brightminds.school.entity;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "roles")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Role {
    @Id @GeneratedValue(strategy = GenerationType.UUID) private UUID id;
    @Column(nullable = false, unique = true) private String name;
    private String description;
    @Column(name = "is_system") @Builder.Default private boolean isSystem = false;
    // Explicit accessor: Jackson strips the "is" prefix from Lombok's isSystem()
    // by default, which would serialize this as "system" instead of "isSystem".
    @JsonProperty("isSystem") public boolean isSystem() { return isSystem; }
    @CreationTimestamp @Column(name = "created_at", updatable = false) private Instant createdAt;

    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(name = "role_permissions",
        joinColumns = @JoinColumn(name = "role_id"),
        inverseJoinColumns = @JoinColumn(name = "permission_id"))
    @Builder.Default private List<Permission> permissions = new ArrayList<>();
}

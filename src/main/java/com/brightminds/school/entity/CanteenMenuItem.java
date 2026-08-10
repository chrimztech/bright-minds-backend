package com.brightminds.school.entity;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "canteen_menu_items")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class CanteenMenuItem {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private String name;

    @Builder.Default
    private String category = "general";

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(precision = 10, scale = 2)
    @Builder.Default
    private BigDecimal price = BigDecimal.ZERO;

    @Column(name = "image_url")
    private String imageUrl;

    @Column(name = "is_available")
    @Builder.Default
    private boolean isAvailable = true;

    // Explicit accessor: Jackson strips the "is" prefix from Lombok's isAvailable()
    // by default, which would serialize this as "available" instead of "isAvailable".
    @JsonProperty("isAvailable")
    public boolean isAvailable() { return isAvailable; }

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private Instant updatedAt;
}

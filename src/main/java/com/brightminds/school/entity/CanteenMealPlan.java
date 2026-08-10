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
@Table(name = "canteen_meal_plans")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class CanteenMealPlan {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private String name;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(name = "meals_per_day")
    @Builder.Default
    private int mealsPerDay = 1;

    @Column(name = "price_per_term", precision = 10, scale = 2)
    @Builder.Default
    private BigDecimal pricePerTerm = BigDecimal.ZERO;

    @Column(name = "is_active")
    @Builder.Default
    private boolean isActive = true;

    // Explicit accessor: Jackson strips the "is" prefix from Lombok's isActive()
    // by default, which would serialize this as "active" instead of "isActive".
    @JsonProperty("isActive")
    public boolean isActive() { return isActive; }

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private Instant updatedAt;
}

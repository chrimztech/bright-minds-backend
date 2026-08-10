package com.brightminds.school.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "library_books")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class LibraryBook {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private String title;

    private String author;
    private String isbn;
    private String category;
    private String shelf;

    @Column(name = "copies_total")
    @Builder.Default
    private int copiesTotal = 1;

    @Column(name = "copies_available")
    @Builder.Default
    private int copiesAvailable = 1;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private Instant updatedAt;
}

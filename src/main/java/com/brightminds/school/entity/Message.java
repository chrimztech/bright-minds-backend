package com.brightminds.school.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "messages")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Message {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String body;

    private String subject;

    @Builder.Default
    private String channel = "sms";

    private String audience;

    // Free-text name/contact of the specific person, only used/required when audience is
    // "INDIVIDUAL" — this feature is a manual communication log, not a real dispatch
    // integration (no email/SMS/WhatsApp is actually sent), so there's no guardian/staff
    // record to link to; the sender just records who they contacted.
    @Column(name = "recipient_label")
    private String recipientLabel;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "class_id")
    private SchoolClass schoolClass;

    @Column(name = "recipient_count")
    private Integer recipientCount;

    @Column(name = "sent_by")
    private UUID sentBy;

    @Column(name = "sent_at")
    private Instant sentAt;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private Instant createdAt;
}

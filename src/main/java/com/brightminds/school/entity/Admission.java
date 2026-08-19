package com.brightminds.school.entity;

import com.brightminds.school.entity.enums.AdmissionStatus;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "admissions")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Admission {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "application_no", unique = true)
    private String applicationNo;

    @Column(name = "full_name", nullable = false)
    private String fullName;

    private String gender;
    private LocalDate dob;

    @Column(name = "previous_school")
    private String previousSchool;

    @Column(name = "parent_name")
    private String parentName;

    @Column(name = "parent_phone")
    private String parentPhone;

    @Column(name = "parent_email")
    private String parentEmail;

    @Column(name = "interview_date")
    private LocalDate interviewDate;

    // The amount actually collected — separate from pupil enrollment, which happens later
    // (or never, for rejected/withdrawn applicants) and is tracked as income the moment it's
    // recorded here, not deferred until enrollment.
    @Column(name = "reg_fee_paid", precision = 10, scale = 2)
    private BigDecimal regFeePaid;

    @Column(name = "reg_fee_paid_on")
    private LocalDate regFeePaidOn;

    @Column(name = "reg_fee_payment_method")
    private String regFeePaymentMethod;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Builder.Default
    private AdmissionStatus status = AdmissionStatus.APPLIED;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "target_class_id")
    private SchoolClass targetClass;

    // Set once this application is converted into an actual Pupil record via the enroll
    // endpoint — prevents double-enrollment and links the two records for traceability.
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "pupil_id")
    private Pupil pupil;

    @Column(columnDefinition = "TEXT")
    private String notes;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private Instant updatedAt;
}

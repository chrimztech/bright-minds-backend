package com.brightminds.school.entity;

import com.brightminds.school.entity.enums.PupilStatus;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "pupils")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Pupil {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "admission_no", nullable = false, unique = true)
    private String admissionNo;

    @Column(name = "full_name", nullable = false)
    private String fullName;

    private String gender;
    private LocalDate dob;
    private String address;
    private String town;
    private String province;

    @Column(name = "postal_code")
    private String postalCode;

    private String nationality;
    private String tribe;
    private String religion;

    @Column(name = "home_language")
    private String homeLanguage;

    @Column(name = "blood_group")
    private String bloodGroup;

    private String allergies;

    @Column(name = "medical_info")
    private String medicalInfo;

    @Column(name = "special_needs")
    private String specialNeeds;

    @Column(name = "nrc_no")
    private String nrcNo;

    @Column(name = "birth_cert_no")
    private String birthCertNo;

    @Column(name = "previous_school")
    private String previousSchool;

    @Column(name = "referral_source")
    private String referralSource;

    @Column(name = "siblings_in_school")
    private String siblingsInSchool;

    private String house;

    @Column(name = "boarding_status")
    private String boardingStatus;

    @Column(name = "transport_mode")
    private String transportMode;

    @Column(name = "emergency_contact")
    private String emergencyContact;

    @Column(name = "photo_url")
    private String photoUrl;

    private String notes;

    @Column(name = "admitted_on")
    @Builder.Default
    private LocalDate admittedOn = LocalDate.now();

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Builder.Default
    private PupilStatus status = PupilStatus.ACTIVE;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "class_id")
    private SchoolClass schoolClass;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private Instant updatedAt;
}

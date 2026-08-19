package com.brightminds.school.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "school_settings")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class SchoolSetting {

    @Id
    private Long id;

    @Column(nullable = false)
    @Builder.Default
    private String name = "Bright Minds School";

    private String address;
    private String city;
    private String district;
    private String province;
    private String country;

    @Column(name = "po_box")
    private String poBox;

    @Column(name = "postal_code")
    private String postalCode;

    @Column(name = "plot_number")
    private String plotNumber;

    private String phone;
    private String email;
    private String website;
    private String motto;

    @Column(name = "head_teacher")
    private String headTeacher;

    @Column(name = "head_teacher_signature_url")
    private String headTeacherSignatureUrl;

    @Column(name = "deputy_head")
    private String deputyHead;

    @Column(name = "registration_no")
    private String registrationNo;

    private String tpin;

    @Column(name = "established_year")
    private Integer establishedYear;

    @Builder.Default
    private String currency = "ZMW";

    @Column(name = "logo_url")
    private String logoUrl;

    @Column(name = "banner_url", columnDefinition = "TEXT")
    private String bannerUrl;

    @Column(name = "map_url")
    private String mapUrl;

    private Double latitude;
    private Double longitude;

    @Column(name = "grading_scale", columnDefinition = "TEXT")
    @Builder.Default
    private String gradingScale = "[]";

    @Column(name = "current_academic_year_id")
    private UUID currentAcademicYearId;

    @Column(name = "current_term_id")
    private UUID currentTermId;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private Instant updatedAt;
}

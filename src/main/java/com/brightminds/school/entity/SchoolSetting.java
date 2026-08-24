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

    // Dashboard hero + quick-links content — editable by SUPER_ADMIN only (see
    // SettingsController#updateDashboard), rendered for every logged-in user.
    @Column(name = "dashboard_hero_heading")
    private String dashboardHeroHeading;

    @Column(name = "dashboard_hero_subtext", columnDefinition = "TEXT")
    private String dashboardHeroSubtext;

    @Column(name = "dashboard_hero_image_url", columnDefinition = "TEXT")
    private String dashboardHeroImageUrl;

    @Column(name = "dashboard_button_label")
    private String dashboardButtonLabel;

    @Column(name = "dashboard_button_url")
    private String dashboardButtonUrl;

    // JSON array of {label, url} quick-link cards, stored as text like gradingScale above.
    @Column(name = "dashboard_links", columnDefinition = "TEXT")
    @Builder.Default
    private String dashboardLinks = "[]";

    // A single optional link button shown on the public login page (e.g. "Visit our website",
    // "Apply for admission") — editable by SUPER_ADMIN only, same as the dashboard content.
    // Exposed pre-login via the minimal PublicController#branding endpoint, not GET /settings.
    @Column(name = "login_button_label")
    private String loginButtonLabel;

    @Column(name = "login_button_url")
    private String loginButtonUrl;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private Instant updatedAt;
}

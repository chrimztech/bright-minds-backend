package com.brightminds.school.controller;

import com.brightminds.school.entity.Inquiry;
import com.brightminds.school.entity.SchoolSetting;
import com.brightminds.school.repository.InquiryRepository;
import com.brightminds.school.repository.PupilRepository;
import com.brightminds.school.repository.SchoolSettingRepository;
import com.brightminds.school.repository.StaffRepository;
import com.brightminds.school.service.PasswordResetService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

// Deliberately separate from SettingsController: GET /settings requires authentication (it
// carries internal fields like current academic year/term IDs), but the login page and the
// public landing page both render before any login exists, so they need their own minimal,
// explicitly public surface — permitted anonymously in SecurityConfig.
@RestController
@RequestMapping("/public")
@RequiredArgsConstructor
@Tag(name = "Public")
public class PublicController {
    private final SchoolSettingRepository repo;
    private final InquiryRepository inquiryRepo;
    private final PupilRepository pupilRepo;
    private final StaffRepository staffRepo;
    private final PasswordResetService passwordResetService;
    private final PasswordEncoder passwordEncoder;

    @GetMapping("/branding")
    public BrandingResponse branding() {
        SchoolSetting s = repo.findById(1L).orElseGet(SchoolSetting::new);
        return new BrandingResponse(
                s.getName(), s.getLogoUrl(), s.getMotto(),
                s.getLoginButtonLabel(), s.getLoginButtonUrl());
    }

    // Everything the public landing page ("/") needs to render — hero, about section and
    // footer contact details — in one call, kept separate from the authenticated /settings
    // payload for the same reason as /branding above.
    @GetMapping("/landing")
    public LandingResponse landing() {
        SchoolSetting s = repo.findById(1L).orElseGet(SchoolSetting::new);
        // Aggregate counts only (no names/records) — safe to show publicly as trust signals
        // ("500+ pupils", "40+ staff") the way most school websites do.
        return new LandingResponse(
                s.getName(), s.getLogoUrl(), s.getMotto(), s.getEstablishedYear(), s.getHeadTeacher(),
                s.getAddress(), s.getCity(), s.getProvince(), s.getCountry(), s.getPhone(), s.getEmail(), s.getWebsite(),
                s.getFacebookUrl(), s.getMapUrl(),
                s.getLandingHeroHeading(), s.getLandingHeroSubtext(), s.getLandingHeroImages(),
                s.getLandingAboutTitle(), s.getLandingAboutBody(),
                pupilRepo.count(), staffRepo.count());
    }

    // Submitted anonymously from the landing page's Inquiries section — a prospective parent
    // or visitor asking a question, with nothing to authenticate against. Staff review these
    // via GET/DELETE /inquiries (InquiryController), which does require a permission.
    @PostMapping("/inquiries")
    @ResponseStatus(HttpStatus.CREATED)
    public Inquiry submitInquiry(@RequestBody InquiryRequest req) {
        if (req.getFullName() == null || req.getFullName().isBlank()) {
            throw new IllegalArgumentException("Name is required");
        }
        if (req.getMessage() == null || req.getMessage().isBlank()) {
            throw new IllegalArgumentException("Message is required");
        }
        return inquiryRepo.save(Inquiry.builder()
                .fullName(req.getFullName().trim())
                .email(req.getEmail())
                .phone(req.getPhone())
                .message(req.getMessage().trim())
                .build());
    }

    // Always responds the same way regardless of whether the email matches an account — see
    // PasswordResetService for why (avoids leaking which emails are registered).
    @PostMapping("/forgot-password")
    public void forgotPassword(@RequestBody ForgotPasswordRequest req) {
        if (req.getEmail() != null && !req.getEmail().isBlank()) {
            passwordResetService.requestReset(req.getEmail().trim());
        }
    }

    @PostMapping("/reset-password")
    public void resetPassword(@Validated @RequestBody ResetPasswordRequest req) {
        passwordResetService.resetPassword(req.getToken(), passwordEncoder.encode(req.getPassword()));
    }

    @Data
    public static class ForgotPasswordRequest {
        private String email;
    }

    @Data
    public static class ResetPasswordRequest {
        @NotBlank
        private String token;
        @NotBlank
        @Size(min = 8, message = "Password must be at least 8 characters")
        private String password;
    }

    public record BrandingResponse(
            String name, String logoUrl, String motto,
            String loginButtonLabel, String loginButtonUrl) {
    }

    public record LandingResponse(
            String name, String logoUrl, String motto, Integer establishedYear, String headTeacher,
            String address, String city, String province, String country, String phone, String email, String website,
            String facebookUrl, String mapUrl,
            String heroHeading, String heroSubtext, String heroImages,
            String aboutTitle, String aboutBody,
            long pupilCount, long staffCount) {
    }

    @Data
    public static class InquiryRequest {
        private String fullName;
        private String email;
        private String phone;
        private String message;
    }
}

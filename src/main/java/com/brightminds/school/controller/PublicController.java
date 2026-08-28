package com.brightminds.school.controller;

import com.brightminds.school.entity.Inquiry;
import com.brightminds.school.entity.SchoolSetting;
import com.brightminds.school.repository.InquiryRepository;
import com.brightminds.school.repository.PupilRepository;
import com.brightminds.school.repository.SchoolSettingRepository;
import com.brightminds.school.repository.StaffRepository;
import com.brightminds.school.service.PasswordResetService;
import com.brightminds.school.service.RateLimiterService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import org.springframework.util.StringUtils;

import java.time.Duration;

// Deliberately separate from SettingsController: GET /settings requires authentication (it
// carries internal fields like current academic year/term IDs), but the login page and the
// public landing page both render before any login exists, so they need their own minimal,
// explicitly public surface — permitted anonymously in SecurityConfig.
@RestController
@RequestMapping("/public")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Public")
public class PublicController {
    private final SchoolSettingRepository repo;
    private final InquiryRepository inquiryRepo;
    private final PupilRepository pupilRepo;
    private final StaffRepository staffRepo;
    private final PasswordResetService passwordResetService;
    private final PasswordEncoder passwordEncoder;
    private final JavaMailSender mailSender;
    private final RateLimiterService rateLimiter;

    @Value("${spring.mail.username:no-reply@brightminds.school}")
    private String mailFrom;

    private static final int FORGOT_PASSWORD_MAX_ATTEMPTS = 3;
    private static final Duration FORGOT_PASSWORD_WINDOW = Duration.ofHours(1);

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
        Inquiry saved = inquiryRepo.save(Inquiry.builder()
                .fullName(req.getFullName().trim())
                .email(req.getEmail())
                .phone(req.getPhone())
                .message(req.getMessage().trim())
                .build());
        notifyInquiry(saved);
        return saved;
    }

    // An inquiry sitting unnoticed in a list nobody's watching isn't "received" in any
    // meaningful sense — the school's own contact address gets an immediate copy so an actual
    // person sees it, and the visitor gets an acknowledgement so they know it went through.
    // Mirrors PasswordResetService's approach: never let a mail failure fail the request itself
    // (blank SMTP credentials in dev, or a misconfigured server in production, shouldn't mean
    // the inquiry itself gets lost — it's already saved above regardless).
    private void notifyInquiry(Inquiry inquiry) {
        SchoolSetting school = repo.findById(1L).orElse(null);
        String schoolEmail = school != null ? school.getEmail() : null;
        String schoolName = school != null && StringUtils.hasText(school.getName()) ? school.getName() : "the school";

        if (StringUtils.hasText(schoolEmail)) {
            try {
                SimpleMailMessage toSchool = new SimpleMailMessage();
                toSchool.setFrom(mailFrom);
                toSchool.setTo(schoolEmail);
                toSchool.setSubject("New website inquiry from " + inquiry.getFullName());
                toSchool.setText(
                        "A new inquiry was submitted on the website:\n\n" +
                        "Name: " + inquiry.getFullName() + "\n" +
                        (StringUtils.hasText(inquiry.getEmail()) ? "Email: " + inquiry.getEmail() + "\n" : "") +
                        (StringUtils.hasText(inquiry.getPhone()) ? "Phone: " + inquiry.getPhone() + "\n" : "") +
                        "\nMessage:\n" + inquiry.getMessage() +
                        "\n\nView and reply to inquiries from the Inquiries page in your dashboard.");
                mailSender.send(toSchool);
            } catch (Exception e) {
                log.warn("Could not email inquiry notification to {}", schoolEmail, e);
            }
        }

        if (StringUtils.hasText(inquiry.getEmail())) {
            try {
                SimpleMailMessage toVisitor = new SimpleMailMessage();
                toVisitor.setFrom(mailFrom);
                toVisitor.setTo(inquiry.getEmail());
                toVisitor.setSubject("We've received your message — " + schoolName);
                toVisitor.setText(
                        "Hello " + inquiry.getFullName() + ",\n\n" +
                        "Thank you for reaching out to " + schoolName + ". We've received your message and " +
                        "someone from our team will get back to you soon.\n\n" +
                        "Your message:\n" + inquiry.getMessage());
                mailSender.send(toVisitor);
            } catch (Exception e) {
                log.warn("Could not email inquiry acknowledgement to {}", inquiry.getEmail(), e);
            }
        }
    }

    // Always responds the same way regardless of whether the email matches an account — see
    // PasswordResetService for why (avoids leaking which emails are registered).
    @PostMapping("/forgot-password")
    public void forgotPassword(@RequestBody ForgotPasswordRequest req, HttpServletRequest httpRequest) {
        // Keyed by IP + email together, not IP alone — a whole school behind one NAT gateway
        // shares a single public IP, so an IP-only key would let requests for one mailbox block
        // every other parent/staff member's own reset request.
        String key = "forgot-password:" + RateLimiterService.clientIp(httpRequest) + ":"
                + (req.getEmail() == null ? "" : req.getEmail().trim().toLowerCase());
        if (!rateLimiter.tryConsume(key, FORGOT_PASSWORD_MAX_ATTEMPTS, FORGOT_PASSWORD_WINDOW)) {
            throw new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS,
                    "Too many reset requests. Please wait a while before trying again.");
        }
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

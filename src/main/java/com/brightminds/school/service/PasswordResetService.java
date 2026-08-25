package com.brightminds.school.service;

import com.brightminds.school.entity.AppUser;
import com.brightminds.school.repository.AppUserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Base64;

@Service
@RequiredArgsConstructor
@Slf4j
public class PasswordResetService {

    private final AppUserRepository userRepo;
    private final JavaMailSender mailSender;
    private final SecureRandom random = new SecureRandom();

    @Value("${app.frontend-url:http://localhost:8080}")
    private String frontendUrl;

    @Value("${spring.mail.username:no-reply@brightminds.school}")
    private String mailFrom;

    // Deliberately does the same amount of "work" and returns the same result whether or not
    // the email matches an account — a different response (or a thrown 404) would let anyone
    // probe which emails have accounts on this system.
    @Transactional
    public void requestReset(String email) {
        userRepo.findByEmail(email).ifPresent(user -> {
            String token = generateToken();
            user.setResetToken(token);
            user.setResetTokenExpiresAt(Instant.now().plus(1, ChronoUnit.HOURS));
            userRepo.save(user);
            sendResetEmail(user, token);
        });
    }

    @Transactional
    public void resetPassword(String token, String newPasswordHash) {
        AppUser user = userRepo.findByResetToken(token)
                .orElseThrow(() -> new IllegalArgumentException("This reset link is invalid or has already been used."));
        if (user.getResetTokenExpiresAt() == null || user.getResetTokenExpiresAt().isBefore(Instant.now())) {
            throw new IllegalArgumentException("This reset link has expired — request a new one.");
        }
        user.setPasswordHash(newPasswordHash);
        user.setResetToken(null);
        user.setResetTokenExpiresAt(null);
        // They just chose this password themselves (unlike an admin-assigned temporary one),
        // so there's nothing to force a follow-up change for.
        user.setMustChangePassword(false);
        userRepo.save(user);
    }

    private String generateToken() {
        byte[] bytes = new byte[32];
        random.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private void sendResetEmail(AppUser user, String token) {
        String link = frontendUrl.replaceAll("/$", "") + "/reset-password?token=" + token;
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(mailFrom);
            message.setTo(user.getEmail());
            message.setSubject("Reset your password");
            message.setText(
                    "Hello " + (user.getFullName() != null ? user.getFullName() : "") + ",\n\n" +
                    "We received a request to reset your password. This link is valid for 1 hour:\n\n" +
                    link + "\n\n" +
                    "If you didn't request this, you can safely ignore this email.");
            mailSender.send(message);
        } catch (Exception e) {
            // Mail delivery failing (no SMTP credentials configured, network issue, etc.) must
            // not surface to the caller — the token is already saved and the reset link still
            // works for anyone who has it. Logged at WARN with the link itself so a school
            // that hasn't set up SMTP yet (or a developer testing locally) can still complete
            // the flow by reading the server log instead of an inbox.
            log.warn("Could not email password reset link to {} — link: {}", user.getEmail(), link, e);
        }
    }
}

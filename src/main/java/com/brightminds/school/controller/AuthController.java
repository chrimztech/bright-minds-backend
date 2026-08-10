package com.brightminds.school.controller;

import com.brightminds.school.dto.AuthRequest;
import com.brightminds.school.dto.AuthResponse;
import com.brightminds.school.dto.RegisterRequest;
import com.brightminds.school.entity.AppUser;
import com.brightminds.school.repository.AppUserRepository;
import com.brightminds.school.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.persistence.EntityNotFoundException;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
@Tag(name = "Authentication")
public class AuthController {

    private final AuthService authService;
    private final AppUserRepository userRepo;
    private final PasswordEncoder passwordEncoder;

    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Register a new user")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public AuthResponse register(@Valid @RequestBody RegisterRequest req) {
        return authService.register(req);
    }

    @PostMapping("/login")
    @Operation(summary = "Login and receive JWT token")
    public AuthResponse login(@Valid @RequestBody AuthRequest req) {
        return authService.login(req);
    }

    @PostMapping("/change-password")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Change own password (authenticated)")
    public void changePassword(@AuthenticationPrincipal UserDetails principal,
                               @Valid @RequestBody ChangePasswordReq req) {
        AppUser user = userRepo.findByEmail(principal.getUsername())
                .orElseThrow(() -> new EntityNotFoundException("User not found"));
        user.setPasswordHash(passwordEncoder.encode(req.getPassword()));
        user.setMustChangePassword(false);
        userRepo.save(user);
    }

    @Data
    public static class ChangePasswordReq {
        @NotBlank
        @Size(min = 8)
        private String password;
    }
}

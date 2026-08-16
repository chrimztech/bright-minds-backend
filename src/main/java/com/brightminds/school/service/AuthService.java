package com.brightminds.school.service;

import com.brightminds.school.dto.AuthRequest;
import com.brightminds.school.dto.AuthResponse;
import com.brightminds.school.dto.RegisterRequest;
import com.brightminds.school.entity.AppUser;
import com.brightminds.school.entity.UserRole;
import com.brightminds.school.repository.AppUserRepository;
import com.brightminds.school.security.CustomUserDetails;
import com.brightminds.school.security.JwtUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final AppUserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authManager;
    private final JwtUtil jwtUtil;
    private final AuditService auditService;

    @Transactional
    public AuthResponse register(RegisterRequest req) {
        if (userRepository.existsByEmail(req.getEmail())) {
            throw new IllegalArgumentException("Email already in use");
        }

        AppUser user = AppUser.builder()
                .email(req.getEmail())
                .passwordHash(passwordEncoder.encode(req.getPassword()))
                .fullName(req.getFullName())
                .phone(req.getPhone())
                .build();

        List<String> roles = req.getRoles() != null && !req.getRoles().isEmpty()
                ? req.getRoles()
                : List.of("ADMIN");

        List<UserRole> userRoles = roles.stream()
                .map(r -> UserRole.builder().user(user).role(r).build())
                .collect(Collectors.toList());
        user.setRoles(userRoles);

        userRepository.save(user);
        CustomUserDetails details = new CustomUserDetails(user);
        return buildResponse(details);
    }

    public AuthResponse login(AuthRequest req) {
        Authentication auth = authManager.authenticate(
                new UsernamePasswordAuthenticationToken(req.getIdentifier(), req.getPassword()));
        CustomUserDetails details = (CustomUserDetails) auth.getPrincipal();
        auditService.log("LOGIN", "AppUser", details.getUser().getId().toString(), details.getUsername());
        return buildResponse(details);
    }

    private AuthResponse buildResponse(CustomUserDetails details) {
        String token = jwtUtil.generateToken(details);
        List<String> roles = details.getAuthorities().stream()
                .map(a -> a.getAuthority().replace("ROLE_", ""))
                .collect(Collectors.toList());
        return AuthResponse.builder()
                .token(token)
                .email(details.getUsername())
                .fullName(details.getUser().getFullName())
                .userId(details.getUser().getId())
                .roles(roles)
                .mustChangePassword(details.getUser().isMustChangePassword())
                .build();
    }
}

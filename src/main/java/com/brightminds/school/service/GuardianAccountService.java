package com.brightminds.school.service;

import com.brightminds.school.entity.AppUser;
import com.brightminds.school.entity.Guardian;
import com.brightminds.school.entity.UserRole;
import com.brightminds.school.entity.enums.AppRole;
import com.brightminds.school.repository.AppUserRepository;
import com.brightminds.school.repository.GuardianRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class GuardianAccountService {

    private final GuardianRepository guardianRepo;
    private final AppUserRepository userRepo;
    private final PasswordEncoder passwordEncoder;
    private final AuditService auditService;

    @Transactional
    public Guardian saveGuardian(Guardian guardian, String temporaryPassword) {
        Guardian saved = guardianRepo.save(guardian);
        if (StringUtils.hasText(saved.getEmail())) {
            AppUser existing = resolveAccount(saved);
            if (existing != null || StringUtils.hasText(temporaryPassword)) {
                linkAccount(saved, existing, temporaryPassword);
            }
        }
        return guardianRepo.save(saved);
    }

    @Transactional
    public Guardian provision(UUID guardianId, String temporaryPassword) {
        if (!StringUtils.hasText(temporaryPassword) || temporaryPassword.length() < 8) {
            throw new IllegalArgumentException("Temporary password must contain at least 8 characters");
        }
        Guardian guardian = guardianRepo.findById(guardianId)
                .orElseThrow(() -> new EntityNotFoundException("Guardian not found"));
        if (!StringUtils.hasText(guardian.getEmail())) {
            throw new IllegalArgumentException("Add an email address before creating a parent login");
        }
        linkAccount(guardian, resolveAccount(guardian), temporaryPassword);
        Guardian saved = guardianRepo.save(guardian);
        auditService.log("PROVISION_PARENT_LOGIN", "Guardian", saved.getId().toString(), saved.getEmail());
        return saved;
    }

    private AppUser resolveAccount(Guardian guardian) {
        if (guardian.getUserId() != null) {
            AppUser linked = userRepo.findById(guardian.getUserId()).orElse(null);
            if (linked != null) return linked;
        }
        return userRepo.findByEmail(guardian.getEmail()).orElse(null);
    }

    private void linkAccount(Guardian guardian, AppUser account, String temporaryPassword) {
        if (account == null) {
            account = AppUser.builder()
                    .email(guardian.getEmail())
                    .fullName(guardian.getFullName())
                    .phone(guardian.getPhone())
                    .passwordHash(passwordEncoder.encode(temporaryPassword))
                    .mustChangePassword(true)
                    .roles(new ArrayList<>())
                    .build();
        } else {
            UUID accountId = account.getId();
            userRepo.findByEmail(guardian.getEmail())
                    .filter(other -> !other.getId().equals(accountId))
                    .ifPresent(other -> { throw new IllegalArgumentException("Email is already used by another account"); });
            account.setEmail(guardian.getEmail());
            account.setFullName(guardian.getFullName());
            account.setPhone(guardian.getPhone());
            if (StringUtils.hasText(temporaryPassword)) {
                account.setPasswordHash(passwordEncoder.encode(temporaryPassword));
                account.setMustChangePassword(true);
            }
        }

        boolean hasParentRole = account.getRoles().stream()
                .anyMatch(role -> role.getRole() == AppRole.PARENT);
        if (!hasParentRole) {
            account.getRoles().add(UserRole.builder().user(account).role(AppRole.PARENT).build());
        }
        AppUser savedAccount = userRepo.save(account);
        guardian.setUserId(savedAccount.getId());
    }
}

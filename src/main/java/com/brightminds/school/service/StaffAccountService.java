package com.brightminds.school.service;

import com.brightminds.school.entity.AppUser;
import com.brightminds.school.entity.Staff;
import com.brightminds.school.entity.UserRole;
import com.brightminds.school.repository.AppUserRepository;
import com.brightminds.school.repository.StaffRepository;
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
public class StaffAccountService {

    private final StaffRepository staffRepo;
    private final AppUserRepository userRepo;
    private final PasswordEncoder passwordEncoder;
    private final AuditService auditService;

    // Called after create/update: only links an account if one already exists for this
    // email, or a temporary password was explicitly supplied — never provisions silently.
    @Transactional
    public Staff sync(Staff staff, String temporaryPassword) {
        if (StringUtils.hasText(staff.getEmail())) {
            AppUser existing = resolveAccount(staff);
            if (existing != null || StringUtils.hasText(temporaryPassword)) {
                linkAccount(staff, existing, temporaryPassword);
            }
        }
        return staffRepo.save(staff);
    }

    @Transactional
    public Staff provision(UUID staffId, String temporaryPassword) {
        if (!StringUtils.hasText(temporaryPassword) || temporaryPassword.length() < 8) {
            throw new IllegalArgumentException("Temporary password must contain at least 8 characters");
        }
        Staff staff = staffRepo.findById(staffId)
                .orElseThrow(() -> new EntityNotFoundException("Staff not found"));
        if (!StringUtils.hasText(staff.getEmail())) {
            throw new IllegalArgumentException("Add an email address before creating a staff login");
        }
        linkAccount(staff, resolveAccount(staff), temporaryPassword);
        Staff saved = staffRepo.save(staff);
        auditService.log("PROVISION_STAFF_LOGIN", "Staff", saved.getId().toString(), saved.getEmail());
        return saved;
    }

    private AppUser resolveAccount(Staff staff) {
        if (staff.getUserId() != null) {
            AppUser linked = userRepo.findById(staff.getUserId()).orElse(null);
            if (linked != null) return linked;
        }
        return userRepo.findByEmail(staff.getEmail()).orElse(null);
    }

    private void linkAccount(Staff staff, AppUser account, String temporaryPassword) {
        if (account == null) {
            account = AppUser.builder()
                    .email(staff.getEmail())
                    .fullName(staff.getFullName())
                    .phone(staff.getPhone())
                    .passwordHash(passwordEncoder.encode(temporaryPassword))
                    .mustChangePassword(true)
                    .roles(new ArrayList<>())
                    .build();
        } else {
            UUID accountId = account.getId();
            userRepo.findByEmail(staff.getEmail())
                    .filter(other -> !other.getId().equals(accountId))
                    .ifPresent(other -> { throw new IllegalArgumentException("Email is already used by another account"); });
            account.setEmail(staff.getEmail());
            account.setFullName(staff.getFullName());
            account.setPhone(staff.getPhone());
            if (StringUtils.hasText(temporaryPassword)) {
                account.setPasswordHash(passwordEncoder.encode(temporaryPassword));
                account.setMustChangePassword(true);
            }
        }

        // Teaching staff automatically get the TEACHER role so they can immediately see
        // "My classes" / their pupils. Non-teaching staff get an account with no role yet —
        // an admin assigns the right role (e.g. ACCOUNTANT, LIBRARIAN) under Users & Roles.
        // The reverse also has to hold: toggling "Is teacher" back off must revoke it, or a
        // staff member moved off teaching duties keeps class-scoped access indefinitely.
        if (staff.isTeacher()) {
            boolean hasTeacherRole = account.getRoles().stream().anyMatch(r -> "TEACHER".equals(r.getRole()));
            if (!hasTeacherRole) account.getRoles().add(UserRole.builder().user(account).role("TEACHER").build());
        } else {
            account.getRoles().removeIf(r -> "TEACHER".equals(r.getRole()));
        }
        AppUser savedAccount = userRepo.save(account);
        staff.setUserId(savedAccount.getId());
    }
}

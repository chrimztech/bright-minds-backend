package com.brightminds.school.config;

import com.brightminds.school.entity.AppUser;
import com.brightminds.school.entity.UserRole;
import com.brightminds.school.entity.enums.AppRole;
import com.brightminds.school.repository.AppUserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class DataInitializer implements ApplicationRunner {

    private final AppUserRepository userRepo;
    private final PasswordEncoder passwordEncoder;

    @Value("${app.bootstrap-admin.email:admin@school.demo}")
    private String adminEmail;

    @Value("${app.bootstrap-admin.password:Admin123!}")
    private String adminPassword;

    @Value("${app.bootstrap-admin.full-name:System Administrator}")
    private String adminFullName;

    @Override
    public void run(ApplicationArguments args) {
        if (userRepo.count() == 0) {
            AppUser admin = AppUser.builder()
                    .email(adminEmail)
                    .passwordHash(passwordEncoder.encode(adminPassword))
                    .fullName(adminFullName)
                    .build();
            UserRole role = UserRole.builder().user(admin).role(AppRole.SUPER_ADMIN).build();
            admin.setRoles(List.of(role));
            userRepo.save(admin);
            log.info("Seeded initial administrator account: {}", adminEmail);
        }
    }
}

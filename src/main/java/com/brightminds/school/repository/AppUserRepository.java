package com.brightminds.school.repository;

import com.brightminds.school.entity.AppUser;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;
import java.util.UUID;

public interface AppUserRepository extends JpaRepository<AppUser, UUID> {
    Optional<AppUser> findByEmail(String email);
    Optional<AppUser> findByPhone(String phone);
    boolean existsByEmail(String email);
    Optional<AppUser> findByResetToken(String resetToken);
}

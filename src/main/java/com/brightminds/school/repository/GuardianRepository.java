package com.brightminds.school.repository;

import com.brightminds.school.entity.Guardian;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface GuardianRepository extends JpaRepository<Guardian, UUID> {
    @Query("SELECT g FROM Guardian g JOIN GuardianPupil gp ON gp.guardian.id = g.id WHERE gp.pupil.id = :pupilId")
    List<Guardian> findByPupilId(UUID pupilId);

    Optional<Guardian> findByEmail(String email);
    Optional<Guardian> findByEmailIgnoreCase(String email);
    Optional<Guardian> findByUserId(UUID userId);
}

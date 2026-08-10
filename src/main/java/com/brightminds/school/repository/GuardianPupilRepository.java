package com.brightminds.school.repository;

import com.brightminds.school.entity.GuardianPupil;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface GuardianPupilRepository extends JpaRepository<GuardianPupil, UUID> {
    List<GuardianPupil> findByPupilId(UUID pupilId);
    List<GuardianPupil> findByGuardianId(UUID guardianId);
    Optional<GuardianPupil> findByGuardianIdAndPupilId(UUID guardianId, UUID pupilId);
    boolean existsByGuardianIdAndPupilId(UUID guardianId, UUID pupilId);
    void deleteByPupilIdAndGuardianId(UUID pupilId, UUID guardianId);
}

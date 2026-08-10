package com.brightminds.school.repository;

import com.brightminds.school.entity.Attendance;
import com.brightminds.school.entity.enums.AttendanceStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface AttendanceRepository extends JpaRepository<Attendance, UUID> {
    List<Attendance> findByDateAndSchoolClassId(LocalDate date, UUID classId);
    Optional<Attendance> findByPupilIdAndDate(UUID pupilId, LocalDate date);
    List<Attendance> findByPupilIdAndDateBetween(UUID pupilId, LocalDate from, LocalDate to);
    List<Attendance> findByPupilIdInAndDateBetweenOrderByDateDesc(List<UUID> pupilIds, LocalDate from, LocalDate to);
    long countByDateAndStatus(LocalDate date, AttendanceStatus status);
    List<Attendance> findByDate(LocalDate date);
}

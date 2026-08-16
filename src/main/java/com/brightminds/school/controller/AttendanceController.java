package com.brightminds.school.controller;

import com.brightminds.school.entity.Attendance;
import com.brightminds.school.entity.Pupil;
import com.brightminds.school.entity.SchoolClass;
import com.brightminds.school.entity.enums.AttendanceStatus;
import com.brightminds.school.repository.AttendanceRepository;
import com.brightminds.school.repository.PupilRepository;
import com.brightminds.school.repository.SchoolClassRepository;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.persistence.EntityNotFoundException;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/attendance")
@RequiredArgsConstructor
@Tag(name = "Attendance")
public class AttendanceController {

    private final AttendanceRepository attendanceRepo;
    private final PupilRepository pupilRepo;
    private final SchoolClassRepository classRepo;

    @PreAuthorize("@perm.has('attendance:view')")
    @GetMapping
    public List<Attendance> list(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @RequestParam(required = false) UUID classId) {
        if (classId != null) return attendanceRepo.findByDateAndSchoolClassId(date, classId);
        return attendanceRepo.findByDate(date);
    }

    @PreAuthorize("@perm.has('attendance:view')")
    @GetMapping("/range")
    public List<Attendance> range(
            @RequestParam UUID classId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return attendanceRepo.findBySchoolClassIdAndDateBetweenOrderByDateAsc(classId, from, to);
    }

    @PreAuthorize("@perm.has('attendance:view')")
    @GetMapping("/pupil/{pupilId}")
    public List<Attendance> byPupil(
            @PathVariable UUID pupilId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return attendanceRepo.findByPupilIdAndDateBetween(pupilId, from, to);
    }

    @PreAuthorize("@perm.has('attendance:mark')")
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Attendance create(@RequestBody AttendanceRequest req) {
        return upsert(req);
    }

    @PreAuthorize("@perm.has('attendance:mark')")
    @PostMapping("/bulk")
    @ResponseStatus(HttpStatus.CREATED)
    public List<Attendance> bulkCreate(@RequestBody List<AttendanceRequest> records) {
        return records.stream().map(this::upsert).toList();
    }

    private Attendance upsert(AttendanceRequest req) {
        Pupil pupil = pupilRepo.findById(req.getPupilId())
                .orElseThrow(() -> new EntityNotFoundException("Pupil not found"));
        // Upsert: update existing record for same pupil+date rather than inserting a duplicate
        Attendance att = attendanceRepo.findByPupilIdAndDate(req.getPupilId(), req.getDate())
                .orElseGet(() -> Attendance.builder().pupil(pupil).date(req.getDate()).build());
        att.setPupil(pupil);
        att.setDate(req.getDate());
        att.setStatus(req.getStatus() != null ? req.getStatus() : AttendanceStatus.PRESENT);
        att.setNotes(req.getNotes());
        if (req.getClassId() != null) {
            classRepo.findById(req.getClassId()).ifPresent(att::setSchoolClass);
        }
        return attendanceRepo.save(att);
    }

    @PreAuthorize("@perm.has('attendance:mark')")
    @PutMapping("/{id}")
    public Attendance update(@PathVariable UUID id, @RequestBody AttendanceRequest req) {
        Attendance att = attendanceRepo.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Attendance not found"));
        if (req.getStatus() != null) att.setStatus(req.getStatus());
        att.setNotes(req.getNotes());
        return attendanceRepo.save(att);
    }

    @Data
    public static class AttendanceRequest {
        private UUID pupilId;
        private UUID classId;
        private LocalDate date;
        private AttendanceStatus status;
        private String notes;
    }
}

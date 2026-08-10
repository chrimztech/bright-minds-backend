package com.brightminds.school.controller;

import com.brightminds.school.entity.TimetableSlot;
import com.brightminds.school.repository.*;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.persistence.EntityNotFoundException;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import java.time.LocalTime;
import java.util.List;
import java.util.UUID;

@RestController @RequestMapping("/timetable") @RequiredArgsConstructor @Tag(name = "Timetable")
public class TimetableController {
    private final TimetableSlotRepository repo;
    private final SchoolClassRepository classRepo;
    private final SubjectRepository subjectRepo;
    private final StaffRepository staffRepo;

    @GetMapping public List<TimetableSlot> list(@RequestParam(required = false) UUID classId) {
        if (classId != null) return repo.findBySchoolClassIdOrderByDayOfWeekAscStartTimeAsc(classId);
        return repo.findAll();
    }
    @PostMapping @ResponseStatus(HttpStatus.CREATED) public TimetableSlot create(@RequestBody SlotRequest req) {
        var sc = classRepo.findById(req.getClassId()).orElseThrow(() -> new EntityNotFoundException("Class not found"));
        var slot = TimetableSlot.builder().schoolClass(sc).dayOfWeek(req.getDayOfWeek()).startTime(req.getStartTime()).endTime(req.getEndTime()).room(req.getRoom()).build();
        if (req.getSubjectId() != null) subjectRepo.findById(req.getSubjectId()).ifPresent(slot::setSubject);
        if (req.getTeacherId() != null) staffRepo.findById(req.getTeacherId()).ifPresent(slot::setTeacher);
        return repo.save(slot);
    }
    @DeleteMapping("/{id}") @ResponseStatus(HttpStatus.NO_CONTENT) public void delete(@PathVariable UUID id) { repo.deleteById(id); }

    @Data public static class SlotRequest {
        private UUID classId; private UUID subjectId; private UUID teacherId;
        private int dayOfWeek; private LocalTime startTime; private LocalTime endTime; private String room;
    }
}

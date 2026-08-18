package com.brightminds.school.controller;

import com.brightminds.school.entity.TimetableSlot;
import com.brightminds.school.repository.*;
import com.brightminds.school.service.ClassScopeService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.persistence.EntityNotFoundException;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import java.time.LocalTime;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@RestController @RequestMapping("/timetable") @RequiredArgsConstructor @Tag(name = "Timetable")
public class TimetableController {
    private final TimetableSlotRepository repo;
    private final SchoolClassRepository classRepo;
    private final SubjectRepository subjectRepo;
    private final StaffRepository staffRepo;
    private final ClassScopeService scopeService;

    @GetMapping public List<TimetableSlot> list(@RequestParam(required = false) UUID classId, Authentication auth) {
        Set<UUID> scope = scopeService.restrictedClassIds(auth);
        if (classId != null) {
            scopeService.assertInScope(scope, classId);
            return repo.findBySchoolClassIdOrderByDayOfWeekAscStartTimeAsc(classId);
        }
        List<TimetableSlot> all = repo.findAll();
        if (scope == null) return all;
        return all.stream().filter(s -> s.getSchoolClass() != null && scope.contains(s.getSchoolClass().getId())).toList();
    }
    @PreAuthorize("@perm.has('timetable:manage')")
    @PostMapping @ResponseStatus(HttpStatus.CREATED) public TimetableSlot create(@RequestBody SlotRequest req, Authentication auth) {
        scopeService.assertInScope(scopeService.restrictedClassIds(auth), req.getClassId());
        var sc = classRepo.findById(req.getClassId()).orElseThrow(() -> new EntityNotFoundException("Class not found"));
        var slot = TimetableSlot.builder().schoolClass(sc).dayOfWeek(req.getDayOfWeek()).startTime(req.getStartTime()).endTime(req.getEndTime()).room(req.getRoom()).build();
        if (req.getSubjectId() != null) subjectRepo.findById(req.getSubjectId()).ifPresent(slot::setSubject);
        if (req.getTeacherId() != null) staffRepo.findById(req.getTeacherId()).ifPresent(slot::setTeacher);
        return repo.save(slot);
    }
    @PreAuthorize("@perm.has('timetable:manage')")
    @DeleteMapping("/{id}") @ResponseStatus(HttpStatus.NO_CONTENT) public void delete(@PathVariable UUID id, Authentication auth) {
        TimetableSlot slot = repo.findById(id).orElseThrow(() -> new EntityNotFoundException("Slot not found"));
        scopeService.assertInScope(scopeService.restrictedClassIds(auth), slot.getSchoolClass() != null ? slot.getSchoolClass().getId() : null);
        repo.deleteById(id);
    }

    @Data public static class SlotRequest {
        private UUID classId; private UUID subjectId; private UUID teacherId;
        private int dayOfWeek; private LocalTime startTime; private LocalTime endTime; private String room;
    }
}

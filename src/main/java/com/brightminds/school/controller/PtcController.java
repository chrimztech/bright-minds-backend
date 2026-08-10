package com.brightminds.school.controller;

import com.brightminds.school.entity.*;
import com.brightminds.school.repository.*;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.persistence.EntityNotFoundException;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/ptc")
@RequiredArgsConstructor
@Tag(name = "PTC Meetings")
public class PtcController {

    private final PtcSessionRepository sessionRepo;
    private final PtcMeetingRepository meetingRepo;
    private final StaffRepository staffRepo;
    private final GuardianRepository guardianRepo;
    private final PupilRepository pupilRepo;
    private final TermRepository termRepo;
    private final AppUserRepository userRepo;

    // ─── Sessions ────────────────────────────────────────────────────────────

    @GetMapping("/sessions")
    public List<PtcSession> listSessions() {
        return sessionRepo.findAllByOrderBySessionDateDesc();
    }

    @PostMapping("/sessions")
    @ResponseStatus(HttpStatus.CREATED)
    public PtcSession createSession(@RequestBody SessionRequest req) {
        Term term = req.getTermId() != null ? termRepo.findById(req.getTermId()).orElse(null) : null;
        return sessionRepo.save(PtcSession.builder()
                .title(req.getTitle())
                .sessionDate(req.getSessionDate())
                .venue(req.getVenue())
                .description(req.getDescription())
                .startTime(req.getStartTime())
                .endTime(req.getEndTime())
                .term(term)
                .build());
    }

    @PutMapping("/sessions/{id}")
    public PtcSession updateSession(@PathVariable UUID id, @RequestBody SessionRequest req) {
        PtcSession s = sessionRepo.findById(id).orElseThrow(EntityNotFoundException::new);
        Term term = req.getTermId() != null ? termRepo.findById(req.getTermId()).orElse(null) : null;
        s.setTitle(req.getTitle());
        s.setSessionDate(req.getSessionDate());
        s.setVenue(req.getVenue());
        s.setDescription(req.getDescription());
        s.setStartTime(req.getStartTime());
        s.setEndTime(req.getEndTime());
        s.setTerm(term);
        return sessionRepo.save(s);
    }

    @DeleteMapping("/sessions/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteSession(@PathVariable UUID id) {
        sessionRepo.deleteById(id);
    }

    // ─── Meetings ─────────────────────────────────────────────────────────────

    @GetMapping("/meetings")
    public List<PtcMeeting> listMeetings(@RequestParam(required = false) UUID sessionId) {
        if (sessionId != null) return meetingRepo.findBySessionIdOrderByMeetingDateAscStartTimeAsc(sessionId);
        return meetingRepo.findAllByOrderByMeetingDateDescStartTimeAsc();
    }

    /** Teacher: their own scheduled meetings. */
    @GetMapping("/meetings/my")
    public List<PtcMeeting> myMeetings(@AuthenticationPrincipal UserDetails principal) {
        AppUser user = userRepo.findByEmail(principal.getUsername()).orElse(null);
        if (user == null) return List.of();
        Staff staff = staffRepo.findByUserId(user.getId()).orElse(null);
        if (staff == null) return List.of();
        return meetingRepo.findByStaffIdOrderByMeetingDateAscStartTimeAsc(staff.getId());
    }

    /** Parent/guardian: their own scheduled meetings. */
    @GetMapping("/meetings/me")
    public List<PtcMeeting> parentMeetings(@AuthenticationPrincipal UserDetails principal) {
        Guardian g = guardianRepo.findByEmail(principal.getUsername()).orElse(null);
        if (g == null) return List.of();
        return meetingRepo.findByGuardianIdOrderByMeetingDateAscStartTimeAsc(g.getId());
    }

    @PostMapping("/meetings")
    @ResponseStatus(HttpStatus.CREATED)
    public PtcMeeting createMeeting(@RequestBody MeetingRequest req) {
        Staff staff = staffRepo.findById(req.getStaffId()).orElseThrow(EntityNotFoundException::new);
        Guardian guardian = guardianRepo.findById(req.getGuardianId()).orElseThrow(EntityNotFoundException::new);
        Pupil pupil = req.getPupilId() != null ? pupilRepo.findById(req.getPupilId()).orElse(null) : null;
        PtcSession session = req.getSessionId() != null ? sessionRepo.findById(req.getSessionId()).orElse(null) : null;
        return meetingRepo.save(PtcMeeting.builder()
                .session(session)
                .staff(staff)
                .guardian(guardian)
                .pupil(pupil)
                .meetingDate(req.getMeetingDate())
                .startTime(req.getStartTime())
                .endTime(req.getEndTime())
                .venue(req.getVenue())
                .agenda(req.getAgenda())
                .status("SCHEDULED")
                .build());
    }

    @PutMapping("/meetings/{id}")
    public PtcMeeting updateMeeting(@PathVariable UUID id, @RequestBody MeetingUpdateRequest req) {
        PtcMeeting m = meetingRepo.findById(id).orElseThrow(EntityNotFoundException::new);
        if (req.getStatus() != null)       m.setStatus(req.getStatus());
        if (req.getTeacherNotes() != null) m.setTeacherNotes(req.getTeacherNotes());
        if (req.getAdminNotes() != null)   m.setAdminNotes(req.getAdminNotes());
        if (req.getAgenda() != null)       m.setAgenda(req.getAgenda());
        if (req.getVenue() != null)        m.setVenue(req.getVenue());
        if (req.getMeetingDate() != null)  m.setMeetingDate(req.getMeetingDate());
        if (req.getStartTime() != null)    m.setStartTime(req.getStartTime());
        if (req.getEndTime() != null)      m.setEndTime(req.getEndTime());
        return meetingRepo.save(m);
    }

    @DeleteMapping("/meetings/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteMeeting(@PathVariable UUID id) {
        meetingRepo.deleteById(id);
    }

    // ─── DTOs ─────────────────────────────────────────────────────────────────

    @Data static class SessionRequest {
        private String title;
        private LocalDate sessionDate;
        private String venue;
        private String description;
        private LocalTime startTime;
        private LocalTime endTime;
        private UUID termId;
    }

    @Data static class MeetingRequest {
        private UUID sessionId;
        private UUID staffId;
        private UUID guardianId;
        private UUID pupilId;
        private LocalDate meetingDate;
        private LocalTime startTime;
        private LocalTime endTime;
        private String venue;
        private String agenda;
    }

    @Data static class MeetingUpdateRequest {
        private String status;
        private String teacherNotes;
        private String adminNotes;
        private String agenda;
        private String venue;
        private LocalDate meetingDate;
        private LocalTime startTime;
        private LocalTime endTime;
    }
}

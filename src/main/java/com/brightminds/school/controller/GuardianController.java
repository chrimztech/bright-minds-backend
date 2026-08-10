package com.brightminds.school.controller;

import com.brightminds.school.dto.ParentPortalDto;
import com.brightminds.school.entity.*;
import com.brightminds.school.repository.*;
import com.brightminds.school.service.GuardianAccountService;
import com.brightminds.school.service.ParentPortalService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.persistence.EntityNotFoundException;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@RestController @RequestMapping("/guardians") @RequiredArgsConstructor @Tag(name = "Guardians")
public class GuardianController {

    private final GuardianRepository repo;
    private final GuardianPupilRepository gpRepo;
    private final PupilRepository pupilRepo;
    private final InvoiceRepository invoiceRepo;
    private final AttendanceRepository attendanceRepo;
    private final MarkRepository markRepo;
    private final ParentPortalService portalService;
    private final GuardianAccountService accountService;

    @GetMapping
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','ADMIN','HEAD_TEACHER','DEPUTY_HEAD')")
    public List<Guardian> list() { return repo.findAll(); }

    @GetMapping("/me")
    @PreAuthorize("hasRole('PARENT')")
    public Guardian me(@AuthenticationPrincipal UserDetails principal) {
        return portalService.currentGuardian(principal);
    }

    @GetMapping("/me/dashboard")
    @PreAuthorize("hasRole('PARENT')")
    public ParentPortalDto.Dashboard myDashboard(@AuthenticationPrincipal UserDetails principal) {
        return portalService.dashboard(principal);
    }

    @GetMapping("/me/pupils")
    @PreAuthorize("hasRole('PARENT')")
    public List<Pupil> myPupils(@AuthenticationPrincipal UserDetails principal) {
        return portalService.children(principal);
    }

    @GetMapping("/me/invoices")
    @PreAuthorize("hasRole('PARENT')")
    public List<Invoice> myInvoices(@AuthenticationPrincipal UserDetails principal) {
        Guardian g = portalService.currentGuardian(principal);
        if (g == null) return List.of();
        List<UUID> pupilIds = gpRepo.findByGuardianId(g.getId()).stream()
                .map(gp -> gp.getPupil().getId()).toList();
        if (pupilIds.isEmpty()) return List.of();
        return invoiceRepo.findByPupilIdInOrderByCreatedAtDesc(pupilIds);
    }

    @GetMapping("/me/attendance")
    @PreAuthorize("hasRole('PARENT')")
    public List<Attendance> myAttendance(
            @AuthenticationPrincipal UserDetails principal,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        Guardian g = portalService.currentGuardian(principal);
        if (g == null) return List.of();
        List<UUID> pupilIds = gpRepo.findByGuardianId(g.getId()).stream()
                .map(gp -> gp.getPupil().getId()).toList();
        if (pupilIds.isEmpty()) return List.of();
        return attendanceRepo.findByPupilIdInAndDateBetweenOrderByDateDesc(pupilIds, from, to);
    }

    @GetMapping("/me/marks")
    @PreAuthorize("hasRole('PARENT')")
    public List<Mark> myMarks(
            @AuthenticationPrincipal UserDetails principal,
            @RequestParam UUID examId) {
        Guardian g = portalService.currentGuardian(principal);
        if (g == null) return List.of();
        List<UUID> pupilIds = gpRepo.findByGuardianId(g.getId()).stream()
                .map(gp -> gp.getPupil().getId()).toList();
        if (pupilIds.isEmpty()) return List.of();
        return markRepo.findByPupilIdInAndExamId(pupilIds, examId);
    }

    @GetMapping("/me/pupils/{pupilId}/attendance")
    @PreAuthorize("hasRole('PARENT')")
    public List<Attendance> childAttendance(
            @AuthenticationPrincipal UserDetails principal,
            @PathVariable UUID pupilId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return portalService.attendance(principal, pupilId, from, to);
    }

    @GetMapping("/me/pupils/{pupilId}/report-cards")
    @PreAuthorize("hasRole('PARENT')")
    public List<ParentPortalDto.ReportCard> childReportCards(
            @AuthenticationPrincipal UserDetails principal,
            @PathVariable UUID pupilId) {
        return portalService.reportCards(principal, pupilId);
    }

    @GetMapping("/pupil/{pupilId}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','ADMIN','HEAD_TEACHER','DEPUTY_HEAD')")
    public List<Guardian> byPupil(@PathVariable UUID pupilId) { return repo.findByPupilId(pupilId); }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','ADMIN','HEAD_TEACHER','DEPUTY_HEAD')")
    public Guardian get(@PathVariable UUID id) { return repo.findById(id).orElseThrow(() -> new EntityNotFoundException("Guardian not found")); }

    @PostMapping @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','ADMIN','HEAD_TEACHER','DEPUTY_HEAD')")
    public Guardian create(@RequestBody GuardianRequest req) {
        Guardian guardian = Guardian.builder().fullName(req.getFullName()).relationship(req.getRelationship())
                .phone(req.getPhone()).email(req.getEmail()).address(req.getAddress())
                .occupation(req.getOccupation()).nationalId(req.getNationalId()).build();
        return accountService.saveGuardian(guardian, req.getTemporaryPassword());
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','ADMIN','HEAD_TEACHER','DEPUTY_HEAD')")
    public Guardian update(@PathVariable UUID id, @RequestBody GuardianRequest req) {
        var g = get(id); g.setFullName(req.getFullName()); g.setRelationship(req.getRelationship());
        g.setPhone(req.getPhone()); g.setEmail(req.getEmail()); g.setAddress(req.getAddress());
        g.setOccupation(req.getOccupation()); g.setNationalId(req.getNationalId());
        return accountService.saveGuardian(g, req.getTemporaryPassword());
    }

    @PostMapping("/{id}/account")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','ADMIN','HEAD_TEACHER','DEPUTY_HEAD')")
    public Guardian provisionAccount(@PathVariable UUID id, @RequestBody ParentAccountRequest req) {
        return accountService.provision(id, req.getTemporaryPassword());
    }

    @DeleteMapping("/{id}") @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','ADMIN','HEAD_TEACHER','DEPUTY_HEAD')")
    public void delete(@PathVariable UUID id) { repo.deleteById(id); }

    @PostMapping("/{guardianId}/pupils/{pupilId}")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','ADMIN','HEAD_TEACHER','DEPUTY_HEAD')")
    public GuardianPupil link(@PathVariable UUID guardianId, @PathVariable UUID pupilId,
                               @RequestParam(defaultValue = "false") boolean isPrimary) {
        var existing = gpRepo.findByGuardianIdAndPupilId(guardianId, pupilId);
        if (existing.isPresent()) return existing.get();
        var g = get(guardianId);
        var p = pupilRepo.findById(pupilId).orElseThrow(() -> new EntityNotFoundException("Pupil not found"));
        return gpRepo.save(GuardianPupil.builder().guardian(g).pupil(p).isPrimary(isPrimary).build());
    }
    @DeleteMapping("/{guardianId}/pupils/{pupilId}") @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','ADMIN','HEAD_TEACHER','DEPUTY_HEAD')")
    public void unlink(@PathVariable UUID guardianId, @PathVariable UUID pupilId) { gpRepo.deleteByPupilIdAndGuardianId(pupilId, guardianId); }

    @Data public static class GuardianRequest {
        private String fullName; private String relationship; private String phone;
        private String email; private String address; private String occupation; private String nationalId;
        private String temporaryPassword;
    }

    @Data public static class ParentAccountRequest { private String temporaryPassword; }
}

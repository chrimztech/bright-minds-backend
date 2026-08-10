package com.brightminds.school.controller;

import com.brightminds.school.entity.AcademicYear;
import com.brightminds.school.entity.Term;
import com.brightminds.school.repository.AcademicYearRepository;
import com.brightminds.school.repository.TermRepository;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.persistence.EntityNotFoundException;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@RestController @RequestMapping("/academic-years") @RequiredArgsConstructor @Tag(name = "Academic Years & Terms")
public class AcademicYearController {
    private final AcademicYearRepository yearRepo;
    private final TermRepository termRepo;

    // GETs left open to all authenticated users: years/terms are referenced as dropdown
    // data by exams, fees, marks, meetings and report-cards across teaching/finance roles.
    @GetMapping public List<AcademicYear> listYears() { return yearRepo.findAll(); }
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','ADMIN','HEAD_TEACHER','DEPUTY_HEAD')")
    @PostMapping @ResponseStatus(HttpStatus.CREATED) public AcademicYear createYear(@RequestBody YearRequest req) {
        return yearRepo.save(AcademicYear.builder().name(req.getName()).startDate(req.getStartDate()).endDate(req.getEndDate()).isCurrent(req.isCurrent()).build()); }
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','ADMIN','HEAD_TEACHER','DEPUTY_HEAD')")
    @DeleteMapping("/{id}") @ResponseStatus(HttpStatus.NO_CONTENT) public void deleteYear(@PathVariable UUID id) { yearRepo.deleteById(id); }

    @GetMapping("/{yearId}/terms") public List<Term> listTerms(@PathVariable UUID yearId) { return termRepo.findByAcademicYearId(yearId); }
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','ADMIN','HEAD_TEACHER','DEPUTY_HEAD')")
    @PostMapping("/{yearId}/terms") @ResponseStatus(HttpStatus.CREATED) public Term createTerm(@PathVariable UUID yearId, @RequestBody TermRequest req) {
        AcademicYear year = yearRepo.findById(yearId).orElseThrow(() -> new EntityNotFoundException("Year not found"));
        return termRepo.save(Term.builder().academicYear(year).name(req.getName()).startDate(req.getStartDate()).endDate(req.getEndDate()).isCurrent(req.isCurrent()).build()); }
    @GetMapping("/terms") public List<Term> allTerms() { return termRepo.findAll(); }
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','ADMIN','HEAD_TEACHER','DEPUTY_HEAD')")
    @DeleteMapping("/terms/{id}") @ResponseStatus(HttpStatus.NO_CONTENT) public void deleteTerm(@PathVariable UUID id) { termRepo.deleteById(id); }

    @Data public static class YearRequest {
        private String name; private LocalDate startDate; private LocalDate endDate;
        private boolean current;
        @JsonProperty("isCurrent") public void setCurrent(boolean current) { this.current = current; }
    }
    @Data public static class TermRequest {
        private String name; private LocalDate startDate; private LocalDate endDate;
        private boolean current;
        @JsonProperty("isCurrent") public void setCurrent(boolean current) { this.current = current; }
    }
}

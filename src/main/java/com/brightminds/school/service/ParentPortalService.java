package com.brightminds.school.service;

import com.brightminds.school.dto.ParentPortalDto;
import com.brightminds.school.entity.*;
import com.brightminds.school.entity.enums.AttendanceStatus;
import com.brightminds.school.repository.*;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ParentPortalService {

    private final GuardianRepository guardianRepo;
    private final GuardianPupilRepository guardianPupilRepo;
    private final AppUserRepository userRepo;
    private final MarkRepository markRepo;
    private final AttendanceRepository attendanceRepo;
    private final TermRepository termRepo;
    private final AcademicYearRepository academicYearRepo;
    private final PupilEnrollmentRepository enrollmentRepo;
    private final ReportCardRemarkRepository remarkRepo;

    public Guardian currentGuardian(UserDetails principal) {
        AppUser user = userRepo.findByEmail(principal.getUsername()).orElse(null);
        if (user != null) {
            Optional<Guardian> linked = guardianRepo.findByUserId(user.getId());
            if (linked.isPresent()) return linked.get();
        }
        return guardianRepo.findByEmailIgnoreCase(principal.getUsername()).orElse(null);
    }

    public List<Pupil> children(UserDetails principal) {
        Guardian guardian = currentGuardian(principal);
        if (guardian == null) return List.of();
        return guardianPupilRepo.findByGuardianId(guardian.getId()).stream()
                .map(GuardianPupil::getPupil)
                .sorted(Comparator.comparing(Pupil::getFullName, String.CASE_INSENSITIVE_ORDER))
                .toList();
    }

    public ParentPortalDto.Dashboard dashboard(UserDetails principal) {
        Guardian guardian = currentGuardian(principal);
        if (guardian == null) return null;

        LocalDate today = LocalDate.now();
        Term currentTerm = termRepo.findByIsCurrentTrue().orElse(null);
        AcademicYear currentYear = academicYearRepo.findByIsCurrentTrue().orElse(null);
        LocalDate from = currentTerm != null
                ? currentTerm.getStartDate()
                : currentYear != null ? currentYear.getStartDate() : today.minusDays(90);
        LocalDate to = currentTerm != null && currentTerm.getEndDate().isBefore(today)
                ? currentTerm.getEndDate() : today;

        List<ParentPortalDto.ChildSummary> children = guardianPupilRepo.findByGuardianId(guardian.getId()).stream()
                .map(GuardianPupil::getPupil)
                .sorted(Comparator.comparing(Pupil::getFullName, String.CASE_INSENSITIVE_ORDER))
                .map(pupil -> childSummary(pupil, from, to))
                .toList();

        return ParentPortalDto.Dashboard.builder()
                .guardian(ParentPortalDto.GuardianInfo.builder()
                        .id(guardian.getId())
                        .fullName(guardian.getFullName())
                        .relationship(guardian.getRelationship())
                        .email(guardian.getEmail())
                        .phone(guardian.getPhone())
                        .build())
                .children(children)
                .build();
    }

    public List<Attendance> attendance(UserDetails principal, UUID pupilId, LocalDate from, LocalDate to) {
        Guardian guardian = requireGuardian(principal);
        requireLinkedChild(guardian, pupilId);
        if (from.isAfter(to)) throw new IllegalArgumentException("Attendance start date must be before end date");
        return attendanceRepo.findByPupilIdAndDateBetween(pupilId, from, to).stream()
                .sorted(Comparator.comparing(Attendance::getDate).reversed())
                .toList();
    }

    public List<ParentPortalDto.ReportCard> reportCards(UserDetails principal, UUID pupilId) {
        Guardian guardian = requireGuardian(principal);
        requireLinkedChild(guardian, pupilId);
        return buildReportCards(pupilId);
    }

    private ParentPortalDto.ChildSummary childSummary(Pupil pupil, LocalDate from, LocalDate to) {
        List<Attendance> records = attendanceRepo.findByPupilIdAndDateBetween(pupil.getId(), from, to);
        List<ParentPortalDto.ReportCard> reportCards = buildReportCards(pupil.getId());
        ParentPortalDto.ReportCard latest = reportCards.isEmpty() ? null : reportCards.get(0);

        return ParentPortalDto.ChildSummary.builder()
                .id(pupil.getId())
                .admissionNo(pupil.getAdmissionNo())
                .fullName(pupil.getFullName())
                .gender(pupil.getGender())
                .dateOfBirth(pupil.getDob())
                .status(pupil.getStatus().name())
                .schoolClass(classInfo(pupil.getSchoolClass()))
                .attendance(attendanceSummary(records, from, to))
                .latestPerformance(latest == null ? null : ParentPortalDto.PerformanceSummary.builder()
                        .examId(latest.getExamId())
                        .examName(latest.getExamName())
                        .termName(latest.getTermName())
                        .academicYearName(latest.getAcademicYearName())
                        .averagePercentage(latest.getAveragePercentage())
                        .grade(latest.getOverallGrade())
                        .build())
                .reportCardCount(reportCards.size())
                .build();
    }

    private List<ParentPortalDto.ReportCard> buildReportCards(UUID pupilId) {
        List<Mark> marks = markRepo.findReportCardMarks(pupilId);
        Map<UUID, List<Mark>> byExam = marks.stream().collect(Collectors.groupingBy(
                mark -> mark.getExam().getId(), LinkedHashMap::new, Collectors.toList()));

        List<ParentPortalDto.ReportCard> cards = byExam.values().stream()
                .map(this::reportCard)
                .collect(Collectors.toCollection(ArrayList::new));
        cards.sort(Comparator.comparing(ParentPortalDto.ReportCard::getExamDate,
                Comparator.nullsLast(Comparator.reverseOrder())));
        return cards;
    }

    private ParentPortalDto.ReportCard reportCard(List<Mark> marks) {
        Mark first = marks.get(0);
        Pupil pupil = first.getPupil();
        Exam exam = first.getExam();
        Term term = exam.getTerm();
        AcademicYear year = term != null ? term.getAcademicYear() : null;
        double outOf = exam.getOutOf() > 0 ? exam.getOutOf() : 100;

        List<ParentPortalDto.SubjectResult> subjects = marks.stream().map(mark -> {
            double percentage = percentage(mark.getScore(), outOf);
            return ParentPortalDto.SubjectResult.builder()
                    .subjectId(mark.getSubject().getId())
                    .subjectName(mark.getSubject().getName())
                    .score(mark.getScore())
                    .outOf(outOf)
                    .percentage(percentage)
                    .grade(gradeFor(percentage))
                    .comment(mark.getComment())
                    .build();
        }).toList();

        double totalScore = marks.stream().mapToDouble(Mark::getScore).sum();
        double totalOutOf = outOf * marks.size();
        double average = percentage(totalScore, totalOutOf);
        LocalDate attendanceFrom = term != null ? term.getStartDate()
                : exam.getExamDate() != null ? exam.getExamDate().minusDays(90) : LocalDate.now().minusDays(90);
        LocalDate attendanceTo = term != null ? term.getEndDate()
                : exam.getExamDate() != null ? exam.getExamDate() : LocalDate.now();
        LocalDate classDate = exam.getExamDate() != null ? exam.getExamDate() : attendanceTo;
        SchoolClass reportClass = enrollmentRepo
                .findFirstByPupilIdAndStartedOnLessThanEqualOrderByStartedOnDesc(pupil.getId(), classDate)
                .map(PupilEnrollment::getSchoolClass)
                .orElse(pupil.getSchoolClass());

        ReportCardRemark remark = remarkRepo.findByPupilIdAndExamId(pupil.getId(), exam.getId()).orElse(null);

        return ParentPortalDto.ReportCard.builder()
                .pupilId(pupil.getId())
                .pupilName(pupil.getFullName())
                .admissionNo(pupil.getAdmissionNo())
                .examId(exam.getId())
                .examName(exam.getName())
                .assessmentType(exam.getAssessmentType() != null ? exam.getAssessmentType().name() : null)
                .examDate(exam.getExamDate())
                .termId(term != null ? term.getId() : null)
                .termName(term != null ? term.getName() : null)
                .academicYearId(year != null ? year.getId() : null)
                .academicYearName(year != null ? year.getName() : null)
                .schoolClass(classInfo(reportClass))
                .subjects(subjects)
                .totalScore(totalScore)
                .totalOutOf(totalOutOf)
                .averagePercentage(average)
                .overallGrade(gradeFor(average))
                .attendance(attendanceSummary(
                        attendanceRepo.findByPupilIdAndDateBetween(pupil.getId(), attendanceFrom, attendanceTo),
                        attendanceFrom, attendanceTo))
                .classTeacherRemark(remark != null ? remark.getClassTeacherRemark() : null)
                .headTeacherRemark(remark != null ? remark.getHeadTeacherRemark() : null)
                .build();
    }

    private ParentPortalDto.AttendanceSummary attendanceSummary(
            List<Attendance> records, LocalDate from, LocalDate to) {
        long present = count(records, AttendanceStatus.PRESENT);
        long absent = count(records, AttendanceStatus.ABSENT);
        long late = count(records, AttendanceStatus.LATE);
        long sick = count(records, AttendanceStatus.SICK);
        long excused = count(records, AttendanceStatus.EXCUSED);
        long total = records.size();
        double attendancePercentage = total == 0 ? 0 : ((present + late) * 100.0) / total;
        return ParentPortalDto.AttendanceSummary.builder()
                .from(from).to(to).total(total).present(present).absent(absent)
                .late(late).sick(sick).excused(excused)
                .percentage(round(attendancePercentage)).build();
    }

    private long count(List<Attendance> records, AttendanceStatus status) {
        return records.stream().filter(record -> record.getStatus() == status).count();
    }

    private ParentPortalDto.ClassInfo classInfo(SchoolClass schoolClass) {
        if (schoolClass == null) return null;
        Staff teacher = schoolClass.getClassTeacher();
        return ParentPortalDto.ClassInfo.builder()
                .id(schoolClass.getId())
                .name(schoolClass.getName())
                .stream(schoolClass.getStream())
                .levelOrder(schoolClass.getLevelOrder())
                .classTeacher(teacher == null ? null : ParentPortalDto.TeacherInfo.builder()
                        .id(teacher.getId())
                        .staffNo(teacher.getStaffNo())
                        .fullName(teacher.getFullName())
                        .email(teacher.getEmail())
                        .phone(teacher.getPhone())
                        .signatureUrl(teacher.getSignatureUrl())
                        .build())
                .build();
    }

    private Guardian requireGuardian(UserDetails principal) {
        Guardian guardian = currentGuardian(principal);
        if (guardian == null) throw new EntityNotFoundException("No guardian profile is linked to this account");
        return guardian;
    }

    private void requireLinkedChild(Guardian guardian, UUID pupilId) {
        if (!guardianPupilRepo.existsByGuardianIdAndPupilId(guardian.getId(), pupilId)) {
            throw new EntityNotFoundException("Child is not linked to this parent account");
        }
    }

    private double percentage(double score, double outOf) {
        return outOf <= 0 ? 0 : round((score / outOf) * 100.0);
    }

    private double round(double value) {
        return Math.round(value * 10.0) / 10.0;
    }

    private String gradeFor(double percentage) {
        if (percentage >= 90) return "A";
        if (percentage >= 75) return "B";
        if (percentage >= 60) return "C";
        if (percentage >= 50) return "D";
        return "E";
    }
}

package com.brightminds.school.service;

import com.brightminds.school.dto.ParentPortalDto;
import com.brightminds.school.entity.*;
import com.brightminds.school.entity.enums.AssessmentType;
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
import java.util.stream.Stream;

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

    // Assessment types that participate in a term report card's 4-column pivot. Ad-hoc exams
    // (MOCK, OPENER, OTHER) can still exist and carry marks, but they're analysis-only — they
    // never appear on a printed report card, admin or parent.
    private static final Set<AssessmentType> TERM_ASSESSMENT_TYPES = EnumSet.of(
            AssessmentType.TEST_1, AssessmentType.TEST_2, AssessmentType.MID_TERM, AssessmentType.END_OF_TERM);

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
                        .termId(latest.getTermId())
                        .termName(latest.getTermName())
                        .academicYearName(latest.getAcademicYearName())
                        .averagePercentage(latest.getTermAveragePercentage())
                        .grade(latest.getOverallGrade())
                        .build())
                .reportCardCount(reportCards.size())
                .build();
    }

    private List<ParentPortalDto.ReportCard> buildReportCards(UUID pupilId) {
        List<Mark> marks = markRepo.findReportCardMarks(pupilId).stream()
                .filter(m -> m.getExam().getTerm() != null
                        && TERM_ASSESSMENT_TYPES.contains(m.getExam().getAssessmentType()))
                .toList();
        Map<UUID, List<Mark>> byTerm = marks.stream().collect(Collectors.groupingBy(
                m -> m.getExam().getTerm().getId(), LinkedHashMap::new, Collectors.toList()));

        return byTerm.values().stream()
                .sorted(Comparator.comparing(
                        (List<Mark> marksInTerm) -> marksInTerm.get(0).getExam().getTerm().getStartDate(),
                        Comparator.nullsLast(Comparator.reverseOrder())))
                .map(this::termReportCard)
                .toList();
    }

    private ParentPortalDto.ReportCard termReportCard(List<Mark> marksInTerm) {
        Mark first = marksInTerm.get(0);
        Pupil pupil = first.getPupil();
        Term term = first.getExam().getTerm();
        AcademicYear year = term.getAcademicYear();

        Map<UUID, List<Mark>> bySubject = marksInTerm.stream().collect(Collectors.groupingBy(
                m -> m.getSubject().getId(), LinkedHashMap::new, Collectors.toList()));
        List<ParentPortalDto.SubjectPivotResult> subjects = bySubject.values().stream()
                .map(this::subjectPivot)
                .sorted(Comparator.comparing(ParentPortalDto.SubjectPivotResult::getSubjectName))
                .toList();

        double termAverage = termAverageFor(marksInTerm);
        LocalDate attendanceFrom = term.getStartDate();
        LocalDate attendanceTo = term.getEndDate();
        LocalDate classDate = attendanceTo != null ? attendanceTo : LocalDate.now();
        SchoolClass reportClass = enrollmentRepo
                .findFirstByPupilIdAndStartedOnLessThanEqualOrderByStartedOnDesc(pupil.getId(), classDate)
                .map(PupilEnrollment::getSchoolClass)
                .orElse(pupil.getSchoolClass());

        ReportCardRemark remark = remarkRepo.findByPupilIdAndTermId(pupil.getId(), term.getId()).orElse(null);
        int[] rank = classRank(term, reportClass, pupil.getId());

        return ParentPortalDto.ReportCard.builder()
                .pupilId(pupil.getId())
                .pupilName(pupil.getFullName())
                .admissionNo(pupil.getAdmissionNo())
                .termId(term.getId())
                .termName(term.getName())
                .academicYearId(year != null ? year.getId() : null)
                .academicYearName(year != null ? year.getName() : null)
                .schoolClass(classInfo(reportClass))
                .subjects(subjects)
                .termAveragePercentage(termAverage)
                .overallGrade(gradeFor(termAverage))
                .attendance(attendanceSummary(
                        attendanceRepo.findByPupilIdAndDateBetween(pupil.getId(), attendanceFrom, attendanceTo),
                        attendanceFrom, attendanceTo))
                .classTeacherRemark(remark != null ? remark.getClassTeacherRemark() : null)
                .headTeacherRemark(remark != null ? remark.getHeadTeacherRemark() : null)
                .position(rank == null ? null : rank[0])
                .classSize(rank == null ? null : rank[1])
                .build();
    }

    private ParentPortalDto.SubjectPivotResult subjectPivot(List<Mark> marksForSubject) {
        Mark any = marksForSubject.get(0);
        Map<AssessmentType, Mark> byType = marksForSubject.stream()
                .collect(Collectors.toMap(m -> m.getExam().getAssessmentType(), m -> m, (a, b) -> b));
        Double average = subjectAverageOnly(marksForSubject);

        return ParentPortalDto.SubjectPivotResult.builder()
                .subjectId(any.getSubject().getId())
                .subjectName(any.getSubject().getName())
                .test1(cellFor(byType.get(AssessmentType.TEST_1)))
                .test2(cellFor(byType.get(AssessmentType.TEST_2)))
                .midTerm(cellFor(byType.get(AssessmentType.MID_TERM)))
                .endOfTerm(cellFor(byType.get(AssessmentType.END_OF_TERM)))
                .averagePercentage(average)
                .grade(average != null ? gradeFor(average) : null)
                .comment(pickComment(byType))
                .build();
    }

    private ParentPortalDto.AssessmentCell cellFor(Mark mark) {
        if (mark == null) return null;
        double outOf = mark.getExam().getOutOf() > 0 ? mark.getExam().getOutOf() : 100;
        return ParentPortalDto.AssessmentCell.builder()
                .score(mark.getScore())
                .outOf(outOf)
                .percentage(percentage(mark.getScore(), outOf))
                .build();
    }

    // Up to 4 candidate comments (one per assessment) collapse into the single "Comment" column
    // the paper report card has per subject — the most recent/most authoritative assessment's
    // comment wins over an earlier one.
    private String pickComment(Map<AssessmentType, Mark> byType) {
        for (AssessmentType type : List.of(AssessmentType.END_OF_TERM, AssessmentType.MID_TERM,
                AssessmentType.TEST_2, AssessmentType.TEST_1)) {
            Mark m = byType.get(type);
            if (m != null && m.getComment() != null && !m.getComment().isBlank()) return m.getComment();
        }
        return null;
    }

    // A pupil's term average = mean of their subjects' own averages (not sum-of-scores, since
    // different assessments can have different outOf). Shared by termReportCard (via
    // subjectAverageOnly, for display) and classRank (for ranking), so a pupil's own report card
    // and their position among classmates can never disagree about what their average actually is.
    private double termAverageFor(List<Mark> marksInTerm) {
        List<Double> subjectAverages = marksInTerm.stream()
                .collect(Collectors.groupingBy(m -> m.getSubject().getId()))
                .values().stream()
                .map(this::subjectAverageOnly)
                .filter(Objects::nonNull)
                .toList();
        return subjectAverages.isEmpty() ? 0
                : round(subjectAverages.stream().mapToDouble(Double::doubleValue).average().orElse(0));
    }

    // A subject's average = mean of whichever of the 4 assessment percentages are present —
    // missing assessments are excluded, never treated as zero.
    private Double subjectAverageOnly(List<Mark> marksForSubject) {
        Map<AssessmentType, Mark> byType = marksForSubject.stream()
                .collect(Collectors.toMap(m -> m.getExam().getAssessmentType(), m -> m, (a, b) -> b));
        List<Double> present = Stream.of(AssessmentType.TEST_1, AssessmentType.TEST_2,
                        AssessmentType.MID_TERM, AssessmentType.END_OF_TERM)
                .map(byType::get)
                .filter(Objects::nonNull)
                .map(m -> percentage(m.getScore(), m.getExam().getOutOf() > 0 ? m.getExam().getOutOf() : 100))
                .toList();
        return present.isEmpty() ? null
                : round(present.stream().mapToDouble(Double::doubleValue).average().orElse(0));
    }

    // Ranks this pupil's term average against classmates who have marks the same term — mirrors
    // the admin report card's ranking. Matched by the pupil's *current* class (not historical
    // enrollment at term time) for consistency with how the admin-facing ranking already does it.
    private int[] classRank(Term term, SchoolClass reportClass, UUID pupilId) {
        if (reportClass == null) return null;
        List<Mark> classMarks = markRepo.findByTermIdAndClassId(term.getId(), reportClass.getId()).stream()
                .filter(m -> TERM_ASSESSMENT_TYPES.contains(m.getExam().getAssessmentType()))
                .toList();
        if (classMarks.isEmpty()) return null;

        Map<UUID, List<Mark>> byPupil = classMarks.stream()
                .collect(Collectors.groupingBy(m -> m.getPupil().getId(), LinkedHashMap::new, Collectors.toList()));

        List<Map.Entry<UUID, Double>> averages = byPupil.entrySet().stream()
                .map(e -> Map.entry(e.getKey(), termAverageFor(e.getValue())))
                .sorted(Comparator.comparingDouble((Map.Entry<UUID, Double> e) -> e.getValue()).reversed())
                .toList();

        for (int i = 0; i < averages.size(); i++) {
            if (averages.get(i).getKey().equals(pupilId)) {
                return new int[] { i + 1, averages.size() };
            }
        }
        return null;
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

package com.brightminds.school.service;

import com.brightminds.school.dto.PageResponse;
import com.brightminds.school.dto.PupilRequest;
import com.brightminds.school.entity.Pupil;
import com.brightminds.school.entity.PupilEnrollment;
import com.brightminds.school.entity.SchoolClass;
import com.brightminds.school.entity.enums.PupilStatus;
import com.brightminds.school.repository.AcademicYearRepository;
import com.brightminds.school.repository.PupilEnrollmentRepository;
import com.brightminds.school.repository.PupilRepository;
import com.brightminds.school.repository.SchoolClassRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.Locale;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class PupilService {

    private final PupilRepository pupilRepo;
    private final SchoolClassRepository classRepo;
    private final AcademicYearRepository yearRepo;
    private final PupilEnrollmentRepository enrollmentRepo;
    private final FeeAutoBillingService feeAutoBillingService;

    @Transactional(readOnly = true)
    public List<Pupil> getAll() {
        return pupilRepo.findAll(Sort.by("fullName"));
    }

    @Transactional(readOnly = true)
    public PageResponse<Pupil> search(String q, int page, int size) {
        var pageable = PageRequest.of(page, size, Sort.by("fullName"));
        if (q == null || q.isBlank()) {
            return PageResponse.of(pupilRepo.findAll(pageable));
        }
        return PageResponse.of(pupilRepo.search(q, pageable));
    }

    @Transactional(readOnly = true)
    public Pupil getById(UUID id) {
        return pupilRepo.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Pupil not found: " + id));
    }

    @Transactional
    public Pupil create(PupilRequest req) {
        String admissionNo = normalizeIdentifier(req.getAdmissionNo());
        if (admissionNo == null) {
            admissionNo = generateAdmissionNo();
        } else if (pupilRepo.existsByAdmissionNo(admissionNo)) {
            throw new IllegalArgumentException("Admission number already exists: " + admissionNo);
        }
        req.setAdmissionNo(admissionNo);
        Pupil pupil = mapToEntity(req, new Pupil());
        Pupil saved = pupilRepo.save(pupil);
        if (saved.getSchoolClass() != null) {
            createEnrollment(saved, saved.getAdmittedOn());
            feeAutoBillingService.billRecurringFeesForClass(saved);
        }
        return saved;
    }

    @Transactional
    public Pupil update(UUID id, PupilRequest req) {
        Pupil pupil = getById(id);
        String admissionNo = normalizeIdentifier(req.getAdmissionNo());
        if (admissionNo == null) {
            req.setAdmissionNo(pupil.getAdmissionNo());
        } else if (!admissionNo.equalsIgnoreCase(pupil.getAdmissionNo())
                && pupilRepo.existsByAdmissionNo(admissionNo)) {
            throw new IllegalArgumentException("Admission number already exists: " + admissionNo);
        } else {
            req.setAdmissionNo(admissionNo);
        }
        UUID previousClassId = pupil.getSchoolClass() != null ? pupil.getSchoolClass().getId() : null;
        mapToEntity(req, pupil);
        Pupil saved = pupilRepo.save(pupil);
        UUID currentClassId = saved.getSchoolClass() != null ? saved.getSchoolClass().getId() : null;
        if (!Objects.equals(previousClassId, currentClassId)) {
            closeEnrollment(saved, LocalDate.now());
            if (saved.getSchoolClass() != null) {
                createEnrollment(saved, LocalDate.now());
                feeAutoBillingService.billRecurringFeesForClass(saved);
            }
        }
        return saved;
    }

    @Transactional
    public void delete(UUID id) {
        pupilRepo.deleteById(id);
    }

    @Transactional(readOnly = true)
    public List<Pupil> getByClass(UUID classId) {
        return pupilRepo.findBySchoolClassId(classId);
    }

    // Only overwrites a field when the request actually carries a value, so callers that
    // submit a partial payload (e.g. the class-roster quick add/remove) don't silently
    // wipe out fields their form doesn't know about. classId is the exception: it is
    // always authoritative, since null there deliberately means "unassign from class".
    private Pupil mapToEntity(PupilRequest req, Pupil p) {
        p.setFullName(req.getFullName());
        if (req.getAdmissionNo() != null) p.setAdmissionNo(req.getAdmissionNo());
        if (req.getGender() != null) p.setGender(req.getGender());
        if (req.getDob() != null) p.setDob(req.getDob());
        if (req.getAddress() != null) p.setAddress(req.getAddress());
        if (req.getTown() != null) p.setTown(req.getTown());
        if (req.getProvince() != null) p.setProvince(req.getProvince());
        if (req.getPostalCode() != null) p.setPostalCode(req.getPostalCode());
        if (req.getNationality() != null) p.setNationality(req.getNationality());
        if (req.getTribe() != null) p.setTribe(req.getTribe());
        if (req.getReligion() != null) p.setReligion(req.getReligion());
        if (req.getHomeLanguage() != null) p.setHomeLanguage(req.getHomeLanguage());
        if (req.getBloodGroup() != null) p.setBloodGroup(req.getBloodGroup());
        if (req.getAllergies() != null) p.setAllergies(req.getAllergies());
        if (req.getMedicalInfo() != null) p.setMedicalInfo(req.getMedicalInfo());
        if (req.getSpecialNeeds() != null) p.setSpecialNeeds(req.getSpecialNeeds());
        if (req.getNrcNo() != null) p.setNrcNo(req.getNrcNo());
        if (req.getBirthCertNo() != null) p.setBirthCertNo(req.getBirthCertNo());
        if (req.getPreviousSchool() != null) p.setPreviousSchool(req.getPreviousSchool());
        if (req.getReferralSource() != null) p.setReferralSource(req.getReferralSource());
        if (req.getSiblingsInSchool() != null) p.setSiblingsInSchool(req.getSiblingsInSchool());
        if (req.getHouse() != null) p.setHouse(req.getHouse());
        if (req.getBoardingStatus() != null) p.setBoardingStatus(req.getBoardingStatus());
        if (req.getTransportMode() != null) p.setTransportMode(req.getTransportMode());
        if (req.getEmergencyContact() != null) p.setEmergencyContact(req.getEmergencyContact());
        if (req.getPhotoUrl() != null) p.setPhotoUrl(req.getPhotoUrl());
        if (req.getNotes() != null) p.setNotes(req.getNotes());
        if (req.getAdmittedOn() != null) p.setAdmittedOn(req.getAdmittedOn());
        if (req.getStatus() != null) p.setStatus(req.getStatus());
        SchoolClass sc = req.getClassId() != null
                ? classRepo.findById(req.getClassId()).orElseThrow(() -> new EntityNotFoundException("Class not found"))
                : null;
        p.setSchoolClass(sc);
        return p;
    }

    private String generateAdmissionNo() {
        String candidate;
        do {
            candidate = "PUP-%d-%s".formatted(
                    LocalDate.now().getYear(),
                    UUID.randomUUID().toString().substring(0, 8).toUpperCase(Locale.ROOT));
        } while (pupilRepo.existsByAdmissionNo(candidate));
        return candidate;
    }

    private String normalizeIdentifier(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private void closeEnrollment(Pupil pupil, LocalDate changedOn) {
        enrollmentRepo.findFirstByPupilIdAndEndedOnIsNullOrderByStartedOnDesc(pupil.getId())
                .ifPresent(enrollment -> {
                    LocalDate endDate = changedOn.isAfter(enrollment.getStartedOn())
                            ? changedOn.minusDays(1) : changedOn;
                    enrollment.setEndedOn(endDate);
                    enrollmentRepo.saveAndFlush(enrollment);
                });
    }

    private void createEnrollment(Pupil pupil, LocalDate startedOn) {
        if (enrollmentRepo.findFirstByPupilIdAndEndedOnIsNullOrderByStartedOnDesc(pupil.getId()).isPresent()) {
            return;
        }
        enrollmentRepo.save(PupilEnrollment.builder()
                .pupil(pupil)
                .schoolClass(pupil.getSchoolClass())
                .academicYear(yearRepo.findByIsCurrentTrue().orElse(null))
                .startedOn(startedOn != null ? startedOn : LocalDate.now())
                .build());
    }
}

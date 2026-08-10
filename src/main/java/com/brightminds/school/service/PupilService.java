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
        if (saved.getSchoolClass() != null) createEnrollment(saved, saved.getAdmittedOn());
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
        if (!Objects.equals(previousClassId, currentClassId) && saved.getSchoolClass() != null) {
            closeEnrollment(saved, LocalDate.now());
            createEnrollment(saved, LocalDate.now());
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

    private Pupil mapToEntity(PupilRequest req, Pupil p) {
        p.setFullName(req.getFullName());
        p.setAdmissionNo(req.getAdmissionNo());
        p.setGender(req.getGender());
        p.setDob(req.getDob());
        p.setAddress(req.getAddress());
        p.setTown(req.getTown());
        p.setProvince(req.getProvince());
        p.setPostalCode(req.getPostalCode());
        p.setNationality(req.getNationality());
        p.setTribe(req.getTribe());
        p.setReligion(req.getReligion());
        p.setHomeLanguage(req.getHomeLanguage());
        p.setBloodGroup(req.getBloodGroup());
        p.setAllergies(req.getAllergies());
        p.setMedicalInfo(req.getMedicalInfo());
        p.setSpecialNeeds(req.getSpecialNeeds());
        p.setNrcNo(req.getNrcNo());
        p.setBirthCertNo(req.getBirthCertNo());
        p.setPreviousSchool(req.getPreviousSchool());
        p.setReferralSource(req.getReferralSource());
        p.setSiblingsInSchool(req.getSiblingsInSchool());
        p.setHouse(req.getHouse());
        p.setBoardingStatus(req.getBoardingStatus());
        p.setTransportMode(req.getTransportMode());
        p.setEmergencyContact(req.getEmergencyContact());
        p.setPhotoUrl(req.getPhotoUrl());
        p.setNotes(req.getNotes());
        if (req.getAdmittedOn() != null) p.setAdmittedOn(req.getAdmittedOn());
        if (req.getStatus() != null) p.setStatus(req.getStatus());
        if (req.getClassId() != null) {
            SchoolClass sc = classRepo.findById(req.getClassId())
                    .orElseThrow(() -> new EntityNotFoundException("Class not found"));
            p.setSchoolClass(sc);
        }
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

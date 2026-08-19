package com.brightminds.school.service;

import com.brightminds.school.dto.PromotionDto;
import com.brightminds.school.entity.*;
import com.brightminds.school.entity.enums.PupilStatus;
import com.brightminds.school.repository.*;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.*;

@Service
@RequiredArgsConstructor
public class PromotionService {

    private final PupilRepository pupilRepo;
    private final SchoolClassRepository classRepo;
    private final AcademicYearRepository yearRepo;
    private final PupilEnrollmentRepository enrollmentRepo;
    private final PupilPromotionRepository promotionRepo;
    private final AppUserRepository userRepo;
    private final AuditService auditService;
    private final FeeAutoBillingService feeAutoBillingService;

    @Transactional
    public PromotionDto.Result promote(PromotionDto.Request request, UserDetails principal) {
        if (request.getPupilIds() == null || request.getPupilIds().isEmpty()) {
            throw new IllegalArgumentException("Select at least one pupil to promote");
        }
        if (request.getTargetClassId() == null) {
            throw new IllegalArgumentException("Select a destination class");
        }

        SchoolClass target = classRepo.findById(request.getTargetClassId())
                .orElseThrow(() -> new EntityNotFoundException("Destination class not found"));
        List<UUID> uniqueIds = request.getPupilIds().stream().distinct().toList();
        List<Pupil> pupils = pupilRepo.findAllById(uniqueIds);
        if (pupils.size() != uniqueIds.size()) {
            throw new EntityNotFoundException("One or more selected pupils were not found");
        }

        for (Pupil pupil : pupils) validatePromotion(pupil, target);
        validateCapacity(target, pupils.size());

        AcademicYear year = request.getAcademicYearId() != null
                ? yearRepo.findById(request.getAcademicYearId())
                    .orElseThrow(() -> new EntityNotFoundException("Academic year not found"))
                : yearRepo.findByIsCurrentTrue().orElse(null);
        LocalDate promotedOn = request.getPromotedOn() != null ? request.getPromotedOn() : LocalDate.now();
        AppUser promotedBy = userRepo.findByEmail(principal.getUsername()).orElse(null);

        List<PromotionDto.Record> records = new ArrayList<>();
        for (Pupil pupil : pupils) {
            SchoolClass source = pupil.getSchoolClass();
            closeCurrentEnrollment(pupil, source, promotedOn);

            pupil.setSchoolClass(target);
            pupilRepo.save(pupil);
            enrollmentRepo.save(PupilEnrollment.builder()
                    .pupil(pupil)
                    .schoolClass(target)
                    .academicYear(year)
                    .startedOn(promotedOn)
                    .build());
            // Same as the single-pupil class-change path in PupilService.update() — bulk
            // promotion moved pupils into a new class without ever billing that class's
            // recurring fees, silently missing a whole cohort's fees at year-end.
            feeAutoBillingService.billRecurringFeesForClass(pupil);

            PupilPromotion promotion = promotionRepo.save(PupilPromotion.builder()
                    .pupil(pupil)
                    .fromClass(source)
                    .toClass(target)
                    .academicYear(year)
                    .promotedOn(promotedOn)
                    .promotedBy(promotedBy)
                    .notes(request.getNotes())
                    .build());
            records.add(toRecord(promotion));
        }

        auditService.log("PROMOTE_PUPILS", "SchoolClass", target.getId().toString(),
                pupils.size() + " pupil(s) promoted to " + classLabel(target));
        PromotionDto.Result result = new PromotionDto.Result();
        result.setPromotedCount(records.size());
        result.setPromotions(records);
        return result;
    }

    @Transactional(readOnly = true)
    public List<PromotionDto.Record> history(UUID pupilId) {
        if (!pupilRepo.existsById(pupilId)) throw new EntityNotFoundException("Pupil not found");
        return promotionRepo.findByPupilIdOrderByPromotedOnDesc(pupilId).stream()
                .map(this::toRecord).toList();
    }

    private void validatePromotion(Pupil pupil, SchoolClass target) {
        if (pupil.getStatus() != PupilStatus.ACTIVE) {
            throw new IllegalArgumentException(pupil.getFullName() + " is not an active pupil");
        }
        SchoolClass source = pupil.getSchoolClass();
        if (source == null) {
            throw new IllegalArgumentException(pupil.getFullName() + " is not assigned to a class");
        }
        if (source.getId().equals(target.getId())) {
            throw new IllegalArgumentException(pupil.getFullName() + " is already in the destination class");
        }

        int nextLevel = classRepo.findAllByOrderByLevelOrderAsc().stream()
                .mapToInt(SchoolClass::getLevelOrder)
                .filter(level -> level > source.getLevelOrder())
                .min()
                .orElseThrow(() -> new IllegalArgumentException(
                        classLabel(source) + " is the highest configured grade"));
        if (target.getLevelOrder() != nextLevel) {
            throw new IllegalArgumentException(
                    pupil.getFullName() + " can only be promoted to the next grade level (order " + nextLevel + ")");
        }
    }

    private void validateCapacity(SchoolClass target, int incoming) {
        if (target.getCapacity() == null || target.getCapacity() <= 0) return;
        long current = pupilRepo.countBySchoolClassId(target.getId());
        if (current + incoming > target.getCapacity()) {
            throw new IllegalArgumentException("Destination class capacity would be exceeded");
        }
    }

    private void closeCurrentEnrollment(Pupil pupil, SchoolClass source, LocalDate promotedOn) {
        PupilEnrollment current = enrollmentRepo
                .findFirstByPupilIdAndEndedOnIsNullOrderByStartedOnDesc(pupil.getId())
                .orElseGet(() -> PupilEnrollment.builder()
                        .pupil(pupil)
                        .schoolClass(source)
                        .startedOn(pupil.getAdmittedOn() != null ? pupil.getAdmittedOn() : promotedOn)
                        .build());
        LocalDate endDate = promotedOn.isAfter(current.getStartedOn())
                ? promotedOn.minusDays(1) : promotedOn;
        current.setEndedOn(endDate);
        enrollmentRepo.saveAndFlush(current);
    }

    private PromotionDto.Record toRecord(PupilPromotion promotion) {
        PromotionDto.Record record = new PromotionDto.Record();
        record.setId(promotion.getId());
        record.setPupilId(promotion.getPupil().getId());
        record.setPupilName(promotion.getPupil().getFullName());
        record.setFromClassId(promotion.getFromClass().getId());
        record.setFromClassName(classLabel(promotion.getFromClass()));
        record.setToClassId(promotion.getToClass().getId());
        record.setToClassName(classLabel(promotion.getToClass()));
        if (promotion.getAcademicYear() != null) {
            record.setAcademicYearId(promotion.getAcademicYear().getId());
            record.setAcademicYearName(promotion.getAcademicYear().getName());
        }
        record.setPromotedOn(promotion.getPromotedOn());
        record.setNotes(promotion.getNotes());
        return record;
    }

    private String classLabel(SchoolClass schoolClass) {
        return schoolClass.getName() + (schoolClass.getStream() == null || schoolClass.getStream().isBlank()
                ? "" : " " + schoolClass.getStream());
    }
}

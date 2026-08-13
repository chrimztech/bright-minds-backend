package com.brightminds.school.service;

import com.brightminds.school.dto.PageResponse;
import com.brightminds.school.dto.StaffRequest;
import com.brightminds.school.entity.Staff;
import com.brightminds.school.entity.enums.StaffStatus;
import com.brightminds.school.repository.StaffRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.time.LocalDate;
import java.util.Locale;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class StaffService {

    private final StaffRepository staffRepo;
    private final StaffAccountService staffAccountService;

    @Transactional(readOnly = true)
    public List<Staff> getAll() {
        return staffRepo.findAll(Sort.by("fullName"));
    }

    @Transactional(readOnly = true)
    public PageResponse<Staff> search(String q, int page, int size) {
        var pageable = PageRequest.of(page, size, Sort.by("fullName"));
        if (q == null || q.isBlank()) return PageResponse.of(staffRepo.findAll(pageable));
        return PageResponse.of(staffRepo.search(q, pageable));
    }

    @Transactional(readOnly = true)
    public Staff getById(UUID id) {
        return staffRepo.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Staff not found: " + id));
    }

    @Transactional
    public Staff create(StaffRequest req) {
        String staffNo = normalizeIdentifier(req.getStaffNo());
        if (staffNo == null) {
            staffNo = generateStaffNo();
        } else if (staffRepo.existsByStaffNo(staffNo)) {
            throw new IllegalArgumentException("Staff number already exists: " + staffNo);
        }
        req.setStaffNo(staffNo);
        Staff saved = staffRepo.save(mapToEntity(req, new Staff()));
        return staffAccountService.sync(saved, req.getTemporaryPassword());
    }

    @Transactional
    public Staff update(UUID id, StaffRequest req) {
        Staff s = getById(id);
        String staffNo = normalizeIdentifier(req.getStaffNo());
        if (staffNo == null) {
            req.setStaffNo(s.getStaffNo());
        } else if (!staffNo.equalsIgnoreCase(s.getStaffNo()) && staffRepo.existsByStaffNo(staffNo)) {
            throw new IllegalArgumentException("Staff number already exists: " + staffNo);
        } else {
            req.setStaffNo(staffNo);
        }
        Staff saved = staffRepo.save(mapToEntity(req, s));
        return staffAccountService.sync(saved, req.getTemporaryPassword());
    }

    @Transactional
    public void delete(UUID id) {
        staffRepo.deleteById(id);
    }

    @Transactional(readOnly = true)
    public List<Staff> getTeachers() {
        return staffRepo.findByIsTeacherTrue();
    }

    private Staff mapToEntity(StaffRequest req, Staff s) {
        s.setFullName(req.getFullName());
        s.setStaffNo(req.getStaffNo());
        s.setTeacher(req.isTeacher());
        if (req.getGender() != null) s.setGender(req.getGender());
        if (req.getEmail() != null) s.setEmail(req.getEmail());
        if (req.getPhone() != null) s.setPhone(req.getPhone());
        if (req.getAddress() != null) s.setAddress(req.getAddress());
        if (req.getDob() != null) s.setDob(req.getDob());
        if (req.getDateJoined() != null) s.setDateJoined(req.getDateJoined());
        if (req.getEmploymentType() != null) s.setEmploymentType(req.getEmploymentType());
        s.setContractEndDate(req.getContractEndDate());
        if (req.getRoleCategory() != null) s.setRoleCategory(req.getRoleCategory());
        if (req.getBasicSalary() != null) s.setBasicSalary(req.getBasicSalary());
        if (req.getQualifications() != null) s.setQualifications(req.getQualifications());
        if (req.getNextOfKin() != null) s.setNextOfKin(req.getNextOfKin());
        if (req.getBankName() != null) s.setBankName(req.getBankName());
        if (req.getBankAccount() != null) s.setBankAccount(req.getBankAccount());
        if (req.getPhotoUrl() != null) s.setPhotoUrl(req.getPhotoUrl());
        if (req.getStatus() != null) s.setStatus(req.getStatus());
        return s;
    }

    private String generateStaffNo() {
        String candidate;
        do {
            candidate = "STF-%d-%s".formatted(
                    LocalDate.now().getYear(),
                    UUID.randomUUID().toString().substring(0, 8).toUpperCase(Locale.ROOT));
        } while (staffRepo.existsByStaffNo(candidate));
        return candidate;
    }

    private String normalizeIdentifier(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}

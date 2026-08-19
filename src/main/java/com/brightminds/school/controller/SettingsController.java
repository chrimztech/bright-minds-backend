package com.brightminds.school.controller;

import com.brightminds.school.entity.SchoolSetting;
import com.brightminds.school.repository.SchoolSettingRepository;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController @RequestMapping("/settings") @RequiredArgsConstructor @Tag(name = "School Settings")
public class SettingsController {
    private final SchoolSettingRepository repo;

    // Left open to all authenticated users: branding/school info is read by printed
    // documents (invoices, report cards) across roles, not just the Settings page.
    @GetMapping
    public SchoolSetting get() {
        return repo.findById(1L).orElseGet(() -> {
            SchoolSetting s = new SchoolSetting();
            s.setId(1L);
            s.setName("Bright Minds School");
            return repo.save(s);
        });
    }

    @PutMapping
    @PreAuthorize("@perm.has('settings:edit')")
    public SchoolSetting update(@RequestBody SettingsRequest req) {
        SchoolSetting s = get();
        if (req.name != null) s.setName(req.name);
        if (req.address != null) s.setAddress(req.address);
        if (req.city != null) s.setCity(req.city);
        if (req.district != null) s.setDistrict(req.district);
        if (req.province != null) s.setProvince(req.province);
        if (req.country != null) s.setCountry(req.country);
        if (req.poBox != null) s.setPoBox(req.poBox);
        if (req.postalCode != null) s.setPostalCode(req.postalCode);
        if (req.plotNumber != null) s.setPlotNumber(req.plotNumber);
        if (req.phone != null) s.setPhone(req.phone);
        if (req.email != null) s.setEmail(req.email);
        if (req.website != null) s.setWebsite(req.website);
        if (req.motto != null) s.setMotto(req.motto);
        if (req.headTeacher != null) s.setHeadTeacher(req.headTeacher);
        if (req.headTeacherSignatureUrl != null) s.setHeadTeacherSignatureUrl(req.headTeacherSignatureUrl);
        if (req.deputyHead != null) s.setDeputyHead(req.deputyHead);
        if (req.registrationNo != null) s.setRegistrationNo(req.registrationNo);
        if (req.tpin != null) s.setTpin(req.tpin);
        if (req.currency != null) s.setCurrency(req.currency);
        if (req.logoUrl != null) s.setLogoUrl(req.logoUrl);
        if (req.bannerUrl != null) s.setBannerUrl(req.bannerUrl);
        if (req.mapUrl != null) s.setMapUrl(req.mapUrl);
        if (req.gradingScale != null) s.setGradingScale(req.gradingScale);
        // Clearable numeric fields: the settings form omits these from the payload
        // (rather than sending null) when the user blanks the input, so they must
        // be applied unconditionally to let the field actually be cleared.
        s.setEstablishedYear(req.establishedYear);
        s.setLatitude(req.latitude);
        s.setLongitude(req.longitude);
        if (req.currentAcademicYearId != null) s.setCurrentAcademicYearId(req.currentAcademicYearId);
        if (req.currentTermId != null) s.setCurrentTermId(req.currentTermId);
        return repo.save(s);
    }

    @Data
    public static class SettingsRequest {
        private String name;
        private String address;
        private String city;
        private String district;
        private String province;
        private String country;
        private String poBox;
        private String postalCode;
        private String plotNumber;
        private String phone;
        private String email;
        private String website;
        private String motto;
        private String headTeacher;
        private String headTeacherSignatureUrl;
        private String deputyHead;
        private String registrationNo;
        private String tpin;
        private Integer establishedYear;
        private String currency;
        private String logoUrl;
        private String bannerUrl;
        private String mapUrl;
        private Double latitude;
        private Double longitude;
        private String gradingScale;
        private UUID currentAcademicYearId;
        private UUID currentTermId;
    }
}

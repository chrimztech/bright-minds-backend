package com.brightminds.school.controller;

import com.brightminds.school.entity.SchoolSetting;
import com.brightminds.school.repository.SchoolSettingRepository;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

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
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','ADMIN','HEAD_TEACHER','DEPUTY_HEAD')")
    public SchoolSetting update(@RequestBody SchoolSetting settings) {
        settings.setId(1L);
        return repo.save(settings);
    }
}

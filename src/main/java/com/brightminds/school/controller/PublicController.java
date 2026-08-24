package com.brightminds.school.controller;

import com.brightminds.school.entity.SchoolSetting;
import com.brightminds.school.repository.SchoolSettingRepository;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

// Deliberately separate from SettingsController: GET /settings requires authentication (it
// carries internal fields like current academic year/term IDs), but the login page renders
// before any login exists, so it needs its own minimal, explicitly public surface — just
// branding plus the optional login-page button, permitted anonymously in SecurityConfig.
@RestController
@RequestMapping("/public")
@RequiredArgsConstructor
@Tag(name = "Public")
public class PublicController {
    private final SchoolSettingRepository repo;

    @GetMapping("/branding")
    public BrandingResponse branding() {
        SchoolSetting s = repo.findById(1L).orElseGet(SchoolSetting::new);
        return new BrandingResponse(
                s.getName(), s.getLogoUrl(), s.getMotto(),
                s.getLoginButtonLabel(), s.getLoginButtonUrl());
    }

    public record BrandingResponse(
            String name, String logoUrl, String motto,
            String loginButtonLabel, String loginButtonUrl) {
    }
}

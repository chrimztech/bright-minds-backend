package com.brightminds.school.controller;

import com.brightminds.school.dto.PromotionDto;
import com.brightminds.school.service.PromotionService;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/promotions")
@RequiredArgsConstructor
@Tag(name = "Pupil Promotions")
@PreAuthorize("@perm.has('promotions:manage')")
public class PromotionController {

    private final PromotionService promotionService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public PromotionDto.Result promote(
            @RequestBody PromotionDto.Request request,
            @AuthenticationPrincipal UserDetails principal) {
        return promotionService.promote(request, principal);
    }

    @GetMapping("/history/{pupilId}")
    public List<PromotionDto.Record> history(@PathVariable UUID pupilId) {
        return promotionService.history(pupilId);
    }
}

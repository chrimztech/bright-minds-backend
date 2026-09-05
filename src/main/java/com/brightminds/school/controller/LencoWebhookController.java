package com.brightminds.school.controller;

import com.brightminds.school.service.LencoService;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

// Public — Lenco calls this directly, with no session of ours to authenticate. Deliberately
// minimal: verify the signature, find *some* reference-shaped field in the payload, and re-check
// that reference's real status via LencoService.checkStatus (which re-queries Lenco itself) —
// this webhook's own claimed event/status is never trusted, only used as a "check again now" nudge.
// Requires the school to email support@lenco.ng with this endpoint's public URL to ever fire;
// polling from the parent portal is what actually drives confirmation regardless.
@RestController
@RequestMapping("/public/webhooks/lenco")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Lenco Webhook")
public class LencoWebhookController {

    private final LencoService lencoService;

    @PostMapping
    public void handle(@RequestBody String rawBody, @RequestHeader(value = "X-Lenco-Signature", required = false) String signature) {
        if (!lencoService.verifySignature(rawBody, signature)) {
            log.warn("Rejected Lenco webhook with invalid or missing signature");
            return; // Still 200 — an invalid signature isn't Lenco's problem to retry.
        }
        String reference = lencoService.extractReference(rawBody);
        if (reference != null) {
            try {
                lencoService.checkStatus(reference);
            } catch (Exception e) {
                log.warn("Lenco webhook re-check failed for reference {}", reference, e);
            }
        }
    }
}

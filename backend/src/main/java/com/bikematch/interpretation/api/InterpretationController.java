package com.bikematch.interpretation.api;

import com.bikematch.interpretation.InterpretationService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/bikes/{bikeId}/interpretation")
public class InterpretationController {

    private final InterpretationService interpretationService;

    public InterpretationController(InterpretationService interpretationService) {
        this.interpretationService = interpretationService;
    }

    @GetMapping
    public InterpretationResponse get(
            @AuthenticationPrincipal String authenticatedUserId,
            @PathVariable long bikeId,
            @RequestParam(defaultValue = "en") String language
    ) {
        return InterpretationResponse.from(
                interpretationService.get(bikeId, viewerId(authenticatedUserId), language));
    }

    @PostMapping
    public ResponseEntity<InterpretationResponse> generate(
            @AuthenticationPrincipal String authenticatedUserId,
            @PathVariable long bikeId,
            @RequestParam(defaultValue = "en") String language
    ) {
        long ownerId = Long.parseLong(authenticatedUserId);
        return ResponseEntity.ok(InterpretationResponse.from(
                interpretationService.generate(bikeId, ownerId, language)));
    }

    private Long viewerId(String authenticatedUserId) {
        if (authenticatedUserId == null || "anonymousUser".equals(authenticatedUserId)) {
            return null;
        }
        return Long.valueOf(authenticatedUserId);
    }
}

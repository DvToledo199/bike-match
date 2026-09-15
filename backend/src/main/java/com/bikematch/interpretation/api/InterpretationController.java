package com.bikematch.interpretation.api;

import com.bikematch.interpretation.InterpretationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
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
@Tag(name = "Interpretation", description = "Plain-language explanation of a saved analysis")
public class InterpretationController {

    private final InterpretationService interpretationService;

    public InterpretationController(InterpretationService interpretationService) {
        this.interpretationService = interpretationService;
    }

    @GetMapping
    @Operation(summary = "Read the saved explanation",
            description = "Public bikes need no token; private bikes only for their owner. "
                    + "Reading never calls the AI provider.")
    @ApiResponse(responseCode = "200", description = "Summary and the figures that support it")
    @ApiResponse(responseCode = "404", content = @Content,
            description = "Bike not visible or explanation not generated yet")
    public InterpretationResponse get(
            @AuthenticationPrincipal String authenticatedUserId,
            @PathVariable long bikeId,
            @RequestParam(defaultValue = "en") String language
    ) {
        return InterpretationResponse.from(
                interpretationService.get(bikeId, viewerId(authenticatedUserId), language));
    }

    @PostMapping
    @Operation(summary = "Generate the explanation",
            description = "Owner only. Reuses the saved explanation when nothing relevant has changed.")
    @ApiResponse(responseCode = "200", description = "Summary and the figures that support it")
    @ApiResponse(responseCode = "401", content = @Content, description = "Missing, invalid or expired token")
    @ApiResponse(responseCode = "404", content = @Content,
            description = "Bike not found, not owned by the caller or not analysed yet")
    @ApiResponse(responseCode = "429", content = @Content,
            description = "Too many generation requests; wait and try again")
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

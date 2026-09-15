package com.bikematch.moderation.api;

import com.bikematch.bike.Bike;
import com.bikematch.moderation.ListPendingBikesService;
import com.bikematch.moderation.ModerateBikePublicationService;
import com.bikematch.moderation.RemoveBikeService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/moderation")
@Tag(name = "Moderation", description = "Review publication requests and remove bikes; requires the MODERATOR role")
public class ModerationController {

    private final ListPendingBikesService listPendingBikesService;
    private final ModerateBikePublicationService moderateBikePublicationService;
    private final RemoveBikeService removeBikeService;

    public ModerationController(
            ListPendingBikesService listPendingBikesService,
            ModerateBikePublicationService moderateBikePublicationService,
            RemoveBikeService removeBikeService
    ) {
        this.listPendingBikesService = listPendingBikesService;
        this.moderateBikePublicationService = moderateBikePublicationService;
        this.removeBikeService = removeBikeService;
    }

    @GetMapping("/pending")
    @Operation(summary = "List bikes waiting for review")
    @ApiResponse(responseCode = "200", description = "Pending bikes, without points or full results")
    @ApiResponse(responseCode = "401", content = @Content, description = "Missing, invalid or expired token")
    @ApiResponse(responseCode = "403", content = @Content, description = "The account does not have the MODERATOR role")
    public List<PendingBikeResponse> listPending() {
        return listPendingBikesService.list().stream()
                .map(PendingBikeResponse::from)
                .toList();
    }

    @PostMapping("/{bikeId}/approve")
    @Operation(summary = "Approve a pending bike", description = "PENDING → PUBLIC")
    @ApiResponse(responseCode = "200", description = "New publication status")
    @ApiResponse(responseCode = "401", content = @Content, description = "Missing, invalid or expired token")
    @ApiResponse(responseCode = "403", content = @Content, description = "The account does not have the MODERATOR role")
    @ApiResponse(responseCode = "404", content = @Content, description = "Bike not found")
    @ApiResponse(responseCode = "409", content = @Content, description = "The bike is not pending review")
    public ModerationDecisionResponse approve(@PathVariable long bikeId) {
        Bike bike = moderateBikePublicationService.approve(bikeId);
        return ModerationDecisionResponse.from(bike);
    }

    @PostMapping("/{bikeId}/reject")
    @Operation(summary = "Reject a pending bike", description = "PENDING → REJECTED")
    @ApiResponse(responseCode = "200", description = "New publication status")
    @ApiResponse(responseCode = "401", content = @Content, description = "Missing, invalid or expired token")
    @ApiResponse(responseCode = "403", content = @Content, description = "The account does not have the MODERATOR role")
    @ApiResponse(responseCode = "404", content = @Content, description = "Bike not found")
    @ApiResponse(responseCode = "409", content = @Content, description = "The bike is not pending review")
    public ModerationDecisionResponse reject(@PathVariable long bikeId) {
        Bike bike = moderateBikePublicationService.reject(bikeId);
        return ModerationDecisionResponse.from(bike);
    }

    @PostMapping("/{bikeId}/remove")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Remove a bike",
            description = "Permanently deletes a pending, public or rejected bike with its photo, marked points, "
                    + "result and explanation. Its owner receives a notice with the reason.")
    @ApiResponse(responseCode = "204", description = "Bike removed and owner notified")
    @ApiResponse(responseCode = "400", content = @Content, description = "Missing reason, or longer than 500 characters")
    @ApiResponse(responseCode = "401", content = @Content, description = "Missing, invalid or expired token")
    @ApiResponse(responseCode = "403", content = @Content, description = "The account does not have the MODERATOR role")
    @ApiResponse(responseCode = "404", content = @Content,
            description = "Bike not found, or private and therefore outside moderation")
    public void remove(@PathVariable long bikeId, @Valid @RequestBody RemoveBikeRequest request) {
        removeBikeService.remove(bikeId, request.reason());
    }
}

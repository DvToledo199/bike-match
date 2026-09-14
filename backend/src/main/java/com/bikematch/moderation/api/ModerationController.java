package com.bikematch.moderation.api;

import com.bikematch.bike.Bike;
import com.bikematch.moderation.ListPendingBikesService;
import com.bikematch.moderation.ModerateBikePublicationService;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/moderation")
public class ModerationController {

    private final ListPendingBikesService listPendingBikesService;
    private final ModerateBikePublicationService moderateBikePublicationService;

    public ModerationController(
            ListPendingBikesService listPendingBikesService,
            ModerateBikePublicationService moderateBikePublicationService
    ) {
        this.listPendingBikesService = listPendingBikesService;
        this.moderateBikePublicationService = moderateBikePublicationService;
    }

    @GetMapping("/pending")
    public List<PendingBikeResponse> listPending() {
        return listPendingBikesService.list().stream()
                .map(PendingBikeResponse::from)
                .toList();
    }

    @PostMapping("/{bikeId}/approve")
    public ModerationDecisionResponse approve(@PathVariable long bikeId) {
        Bike bike = moderateBikePublicationService.approve(bikeId);
        return ModerationDecisionResponse.from(bike);
    }

    @PostMapping("/{bikeId}/reject")
    public ModerationDecisionResponse reject(@PathVariable long bikeId) {
        Bike bike = moderateBikePublicationService.reject(bikeId);
        return ModerationDecisionResponse.from(bike);
    }
}

package com.bikematch.moderation.api;

import com.bikematch.moderation.ListPendingBikesService;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/moderation")
public class ModerationController {

    private final ListPendingBikesService listPendingBikesService;

    public ModerationController(ListPendingBikesService listPendingBikesService) {
        this.listPendingBikesService = listPendingBikesService;
    }

    @GetMapping("/pending")
    public List<PendingBikeResponse> listPending() {
        return listPendingBikesService.list().stream()
                .map(PendingBikeResponse::from)
                .toList();
    }
}

package com.bikematch.bike.api;

import com.bikematch.bike.ListOwnedBikesService;
import java.util.List;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/my-bikes")
public class MyBikesController {

    private final ListOwnedBikesService listOwnedBikesService;

    public MyBikesController(ListOwnedBikesService listOwnedBikesService) {
        this.listOwnedBikesService = listOwnedBikesService;
    }

    @GetMapping
    public List<MyBikeSummaryResponse> list(
            @AuthenticationPrincipal String authenticatedUserId
    ) {
        long ownerId = Long.parseLong(authenticatedUserId);

        return listOwnedBikesService.list(ownerId).stream()
                .map(MyBikeSummaryResponse::from)
                .toList();
    }
}

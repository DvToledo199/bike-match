package com.bikematch.bike.api;

import com.bikematch.bike.ListOwnedBikesService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/my-bikes")
@Tag(name = "My bikes", description = "Bikes owned by the authenticated user")
public class MyBikesController {

    private final ListOwnedBikesService listOwnedBikesService;

    public MyBikesController(ListOwnedBikesService listOwnedBikesService) {
        this.listOwnedBikesService = listOwnedBikesService;
    }

    @GetMapping
    @Operation(summary = "List my saved bikes",
            description = "Summaries only; open a bike to see its curves.")
    @ApiResponse(responseCode = "200", description = "The caller's bikes, most recent first")
    @ApiResponse(responseCode = "401", content = @Content, description = "Missing, invalid or expired token")
    public List<MyBikeSummaryResponse> list(
            @AuthenticationPrincipal String authenticatedUserId
    ) {
        long ownerId = Long.parseLong(authenticatedUserId);

        return listOwnedBikesService.list(ownerId).stream()
                .map(MyBikeSummaryResponse::from)
                .toList();
    }
}

package com.bikematch.bike.api;

import com.bikematch.bike.Bike;
import com.bikematch.bike.CreateBikeService;
import jakarta.validation.Valid;
import java.net.URI;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/bikes")
public class BikeController {

    private final CreateBikeService createBikeService;

    public BikeController(CreateBikeService createBikeService) {
        this.createBikeService = createBikeService;
    }

    @PostMapping
    public ResponseEntity<CreateBikeResponse> create(
            @AuthenticationPrincipal String authenticatedUserId,
            @Valid @RequestBody CreateBikeRequest request
    ) {
        long ownerId = Long.parseLong(authenticatedUserId);
        Bike bike = createBikeService.create(ownerId, request.toDetails());
        CreateBikeResponse response = new CreateBikeResponse(bike.getId(), bike.getStatus());
        return ResponseEntity
                .created(URI.create("/api/bikes/" + bike.getId()))
                .body(response);
    }
}

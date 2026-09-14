package com.bikematch.bike.api;

import com.bikematch.bike.AttachBikePhotoService;
import com.bikematch.bike.Bike;
import com.bikematch.bike.BikePhotoFile;
import com.bikematch.bike.CreateBikeService;
import com.bikematch.bike.FinalizeBikeAnalysisService;
import com.bikematch.bike.InvalidBikePhotoException;
import com.bikematch.kinematics.api.PreviewResponse;
import jakarta.validation.Valid;
import java.io.IOException;
import java.net.URI;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/bikes")
public class BikeController {

    private final CreateBikeService createBikeService;
    private final AttachBikePhotoService attachBikePhotoService;
    private final FinalizeBikeAnalysisService finalizeBikeAnalysisService;

    public BikeController(
            CreateBikeService createBikeService,
            AttachBikePhotoService attachBikePhotoService,
            FinalizeBikeAnalysisService finalizeBikeAnalysisService
    ) {
        this.createBikeService = createBikeService;
        this.attachBikePhotoService = attachBikePhotoService;
        this.finalizeBikeAnalysisService = finalizeBikeAnalysisService;
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

    @PostMapping(path = "/{bikeId}/photo", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public BikePhotoResponse attachPhoto(
            @AuthenticationPrincipal String authenticatedUserId,
            @PathVariable long bikeId,
            @RequestPart("photo") MultipartFile photo
    ) {
        long ownerId = Long.parseLong(authenticatedUserId);
        try {
            Bike bike = attachBikePhotoService.attach(
                    ownerId,
                    bikeId,
                    new BikePhotoFile(photo.getBytes(), photo.getContentType()));
            return new BikePhotoResponse(bike.getId(), bike.getPhotoUrl());
        } catch (IOException exception) {
            throw new InvalidBikePhotoException("Photo could not be read", exception);
        }
    }

    @PostMapping("/{bikeId}/analysis")
    public ResponseEntity<PreviewResponse> finalizeAnalysis(
            @AuthenticationPrincipal String authenticatedUserId,
            @PathVariable long bikeId,
            @Valid @RequestBody FinalizeBikeAnalysisRequest request
    ) {
        long ownerId = Long.parseLong(authenticatedUserId);
        PreviewResponse response = finalizeBikeAnalysisService.finalizeAnalysis(
                ownerId, bikeId, request.toGeometry());
        return ResponseEntity
                .created(URI.create("/api/bikes/" + bikeId + "/analysis"))
                .body(response);
    }
}

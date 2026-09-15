package com.bikematch.bike.api;

import com.bikematch.bike.AttachBikePhotoService;
import com.bikematch.bike.Bike;
import com.bikematch.bike.BikePhotoFile;
import com.bikematch.bike.CreateBikeService;
import com.bikematch.bike.FinalizeBikeAnalysisService;
import com.bikematch.bike.GetBikeDetailService;
import com.bikematch.bike.InvalidBikePhotoException;
import com.bikematch.bike.PublishBikeService;
import com.bikematch.kinematics.api.PreviewResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.io.IOException;
import java.net.URI;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/bikes")
@Tag(name = "Bikes", description = "Create, analyse, publish and view saved bikes")
public class BikeController {

    private final CreateBikeService createBikeService;
    private final AttachBikePhotoService attachBikePhotoService;
    private final FinalizeBikeAnalysisService finalizeBikeAnalysisService;
    private final PublishBikeService publishBikeService;
    private final GetBikeDetailService getBikeDetailService;

    public BikeController(
            CreateBikeService createBikeService,
            AttachBikePhotoService attachBikePhotoService,
            FinalizeBikeAnalysisService finalizeBikeAnalysisService,
            PublishBikeService publishBikeService,
            GetBikeDetailService getBikeDetailService
    ) {
        this.createBikeService = createBikeService;
        this.attachBikePhotoService = attachBikePhotoService;
        this.finalizeBikeAnalysisService = finalizeBikeAnalysisService;
        this.publishBikeService = publishBikeService;
        this.getBikeDetailService = getBikeDetailService;
    }

    @GetMapping("/{bikeId}")
    @Operation(summary = "Get a bike's detail",
            description = "Public bikes need no token. Private, pending and rejected bikes are only "
                    + "returned to their owner, so authorize as the owner to see them.")
    @ApiResponse(responseCode = "200", description = "Photo, bike data and saved kinematics result")
    @ApiResponse(responseCode = "404", content = @Content, description = "Bike not found or not visible to the caller")
    public BikeDetailResponse getDetail(
            @AuthenticationPrincipal String authenticatedUserId,
            @PathVariable long bikeId
    ) {
        Long viewerId = authenticatedUserId == null
                ? null
                : "anonymousUser".equals(authenticatedUserId)
                        ? null
                        : Long.valueOf(authenticatedUserId);
        return BikeDetailResponse.from(getBikeDetailService.get(bikeId, viewerId));
    }

    @PostMapping
    @Operation(summary = "Create a private bike",
            description = "Saves the bike data. Upload its photo and save its analysis next.")
    @ApiResponse(responseCode = "201", description = "Bike created with status PRIVATE")
    @ApiResponse(responseCode = "400", content = @Content, description = "Invalid bike data")
    @ApiResponse(responseCode = "401", content = @Content, description = "Missing, invalid or expired token")
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
    @Operation(summary = "Upload the bike photo",
            description = "JPG, PNG or WebP up to 10 MB. The photo can be replaced until the analysis is saved.")
    @ApiResponse(responseCode = "200", description = "Photo stored")
    @ApiResponse(responseCode = "400", content = @Content, description = "Missing, unreadable or unsupported photo")
    @ApiResponse(responseCode = "401", content = @Content, description = "Missing, invalid or expired token")
    @ApiResponse(responseCode = "404", content = @Content, description = "Bike not found or not owned by the caller")
    @ApiResponse(responseCode = "409", content = @Content,
            description = "The analysis is already saved, so the photo is locked")
    @ApiResponse(responseCode = "413", content = @Content, description = "Photo larger than 10 MB")
    @ApiResponse(responseCode = "502", content = @Content, description = "The image storage service failed")
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
    @Operation(summary = "Save the bike analysis",
            description = "Calculates the curves from the marked points and saves points and result together. "
                    + "Photo and points are locked afterwards.")
    @ApiResponse(responseCode = "201", description = "Analysis calculated and saved")
    @ApiResponse(responseCode = "400", content = @Content, description = "Invalid points or impossible geometry")
    @ApiResponse(responseCode = "401", content = @Content, description = "Missing, invalid or expired token")
    @ApiResponse(responseCode = "404", content = @Content, description = "Bike not found or not owned by the caller")
    @ApiResponse(responseCode = "409", content = @Content,
            description = "The bike has no photo yet or its analysis is already saved")
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

    @PostMapping("/{bikeId}/publish")
    @Operation(summary = "Request publication",
            description = "Moves a private bike to PENDING until a moderator reviews it.")
    @ApiResponse(responseCode = "200", description = "Current publication status")
    @ApiResponse(responseCode = "401", content = @Content, description = "Missing, invalid or expired token")
    @ApiResponse(responseCode = "403", content = @Content, description = "Only the owner can request publication")
    @ApiResponse(responseCode = "404", content = @Content, description = "Bike not found")
    public PublishBikeResponse publish(
            @AuthenticationPrincipal String authenticatedUserId,
            @PathVariable long bikeId
    ) {
        long ownerId = Long.parseLong(authenticatedUserId);
        Bike bike = publishBikeService.publish(ownerId, bikeId);
        return PublishBikeResponse.from(bike);
    }
}

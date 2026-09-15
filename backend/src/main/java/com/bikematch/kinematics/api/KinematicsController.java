package com.bikematch.kinematics.api;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/kinematics")
@Tag(name = "Kinematics", description = "Stateless analysis from marked points; nothing is saved")
@SecurityRequirements
public class KinematicsController {

    private final KinematicsService service;

    public KinematicsController(KinematicsService service) {
        this.service = service;
    }

    @PostMapping("/preview")
    @Operation(summary = "Calculate kinematics from six marked points",
            description = "Public preview used by the analysis wizard before a bike is saved.")
    @ApiResponse(responseCode = "200", description = "Curves, descriptors and reference conditions")
    @ApiResponse(responseCode = "400", content = @Content,
            description = "Invalid points, measurements or impossible geometry")
    public PreviewResponse preview(@Valid @RequestBody PreviewRequest request) {
        return service.preview(request);
    }
}

package com.bikematch.health;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/health")
@Tag(name = "Health", description = "Service status")
@SecurityRequirements
public class HealthController {

    private final String version;

    public HealthController(@Value("${app.version}") String version) {
        this.version = version;
    }

    @GetMapping
    @Operation(summary = "Check that the API is running")
    @ApiResponse(responseCode = "200", description = "Status and application version")
    public HealthResponse health() {
        return new HealthResponse("UP", version);
    }

    public record HealthResponse(String status, String version) {
    }
}

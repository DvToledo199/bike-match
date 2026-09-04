package com.bikematch.kinematics.api;

import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/kinematics")
public class KinematicsController {

    private final KinematicsService service;

    public KinematicsController(KinematicsService service) {
        this.service = service;
    }

    @PostMapping("/preview")
    public PreviewResponse preview(@Valid @RequestBody PreviewRequest request) {
        return service.preview(request);
    }
}
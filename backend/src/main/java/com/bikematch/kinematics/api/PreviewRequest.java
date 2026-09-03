package com.bikematch.kinematics.api;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.util.List;

public record PreviewRequest(
        @NotEmpty @Valid List<PointDto> points,
        @Positive double eyeToEyeMm,
        @NotNull @Valid KinematicsParametersDto parameters
) {
}
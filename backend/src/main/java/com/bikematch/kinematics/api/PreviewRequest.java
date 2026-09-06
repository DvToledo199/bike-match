package com.bikematch.kinematics.api;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.DecimalMax;

import java.util.List;

public record PreviewRequest(
        @NotEmpty @Size(min = 6, max = 6) List<@NotNull @Valid PointDto> points,
        @NotNull @DecimalMin("100") @DecimalMax("300") Double eyeToEyeMm,
        @NotNull @Valid KinematicsParametersDto parameters
) {
}

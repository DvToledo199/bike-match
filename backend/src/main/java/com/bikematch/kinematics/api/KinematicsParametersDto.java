package com.bikematch.kinematics.api;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record KinematicsParametersDto(
        @NotNull @DecimalMin("20") @DecimalMax("120") Double shockStrokeMm,
        @NotNull @Min(20) @Max(60) Integer chainringTeeth,
        @NotNull @Min(10) @Max(60) Integer sprocketTeeth,
        @NotNull @DecimalMin("50") @DecimalMax("250") Double declaredTravelMm,
        @NotNull @DecimalMin("10") @DecimalMax("50") Double sagPercent
) {
}

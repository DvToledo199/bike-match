package com.bikematch.kinematics.api;

import jakarta.validation.constraints.Positive;

public record KinematicsParametersDto(
        @Positive double shockStrokeMm,
        @Positive int chainringTeeth,
        @Positive int sprocketTeeth,
        @Positive double declaredTravelMm,
        @Positive double sagPercent
) {
}
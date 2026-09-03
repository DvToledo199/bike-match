package com.bikematch.kinematics.api;

import com.bikematch.kinematics.model.PointType;
import jakarta.validation.constraints.NotNull;

public record PointDto(
        @NotNull PointType type,
        double x,
        double y
) {
}
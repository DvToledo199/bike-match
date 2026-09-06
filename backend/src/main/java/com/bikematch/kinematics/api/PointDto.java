package com.bikematch.kinematics.api;

import com.bikematch.kinematics.model.PointType;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.DecimalMax;

public record PointDto(
        @NotNull PointType type,
        @NotNull @DecimalMin("0") @DecimalMax("100000") Double x,
        @NotNull @DecimalMin("0") @DecimalMax("100000") Double y
) {
}

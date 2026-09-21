package com.bikematch.kinematics.api;

import com.bikematch.bike.SuspensionLayout;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.DecimalMax;

import java.util.List;

public record PreviewRequest(
        @NotEmpty @Size(min = 6, max = 10) List<@NotNull @Valid PointDto> points,
        @NotNull @DecimalMin("100") @DecimalMax("300") Double eyeToEyeMm,
        @NotNull @Valid KinematicsParametersDto parameters,
        SuspensionLayout suspensionLayout
) {
    public PreviewRequest {
        if (suspensionLayout == null) {
            suspensionLayout = SuspensionLayout.SINGLE_PIVOT;
        }
    }

    public PreviewRequest(List<PointDto> points, Double eyeToEyeMm, KinematicsParametersDto parameters) {
        this(points, eyeToEyeMm, parameters, SuspensionLayout.SINGLE_PIVOT);
    }
}

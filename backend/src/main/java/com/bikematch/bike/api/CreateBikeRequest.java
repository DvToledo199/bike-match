package com.bikematch.bike.api;

import com.bikematch.bike.BikeCategory;
import com.bikematch.bike.BikeDetails;
import com.bikematch.bike.CassetteType;
import com.bikematch.bike.SuspensionLayout;
import com.bikematch.kinematics.model.WheelConfiguration;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CreateBikeRequest(
        @NotBlank @Size(max = 80) String brand,
        @NotBlank @Size(max = 100) String model,
        @Min(1900) @Max(2100) Short modelYear,
        @NotNull BikeCategory category,
        @NotNull SuspensionLayout suspensionLayout,
        @NotNull @DecimalMin("50") @DecimalMax("250") Double declaredTravelMm,
        @NotNull @DecimalMin("100") @DecimalMax("300") Double shockEyeToEyeMm,
        @NotNull @DecimalMin("20") @DecimalMax("120") Double shockStrokeMm,
        @NotNull WheelConfiguration wheelConfiguration,
        @NotNull CassetteType cassetteType,
        @NotNull @Min(20) @Max(60) Integer chainringTeeth,
        @NotNull @Min(10) @Max(60) Integer sprocketTeeth,
        @NotNull @DecimalMin("10") @DecimalMax("50") Double sagPercent
) {
    BikeDetails toDetails() {
        return new BikeDetails(
                brand, model, modelYear, category, suspensionLayout,
                declaredTravelMm, shockEyeToEyeMm, shockStrokeMm,
                wheelConfiguration, cassetteType,
                chainringTeeth.shortValue(), sprocketTeeth.shortValue(), sagPercent);
    }
}

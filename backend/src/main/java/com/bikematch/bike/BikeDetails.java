package com.bikematch.bike;

import com.bikematch.kinematics.model.WheelConfiguration;
import java.util.Objects;

public record BikeDetails(
        String brand,
        String model,
        Short modelYear,
        BikeCategory category,
        SuspensionLayout suspensionLayout,
        double declaredTravelMm,
        double shockEyeToEyeMm,
        double shockStrokeMm,
        WheelConfiguration wheelConfiguration,
        CassetteType cassetteType,
        short chainringTeeth,
        short sprocketTeeth,
        double sagPercent
) {
    public BikeDetails {
        brand = normalizedText(brand, "Brand");
        model = normalizedText(model, "Model");
        Objects.requireNonNull(category, "Category is required");
        Objects.requireNonNull(suspensionLayout, "Suspension layout is required");
        Objects.requireNonNull(wheelConfiguration, "Wheel configuration is required");
        Objects.requireNonNull(cassetteType, "Cassette type is required");
        requireRange(modelYear, 1900, 2100, "Model year");
        requireRange(declaredTravelMm, 50, 250, "Declared travel");
        requireRange(shockEyeToEyeMm, 100, 300, "Shock eye-to-eye");
        requireRange(shockStrokeMm, 20, 120, "Shock stroke");
        requireRange(chainringTeeth, 28, 38, "Chainring teeth");
        requireRange(sprocketTeeth, 10, 60, "Sprocket teeth");
        requireRange(sagPercent, 10, 50, "Sag");
    }

    private static String normalizedText(String value, String name) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(name + " is required");
        }
        return value.strip();
    }

    private static void requireRange(double value, double minimum, double maximum, String name) {
        if (!Double.isFinite(value) || value < minimum || value > maximum) {
            throw new IllegalArgumentException(
                    name + " must be between " + minimum + " and " + maximum);
        }
    }

    private static void requireRange(Short value, int minimum, int maximum, String name) {
        if (value != null) {
            requireRange(value.doubleValue(), minimum, maximum, name);
        }
    }
}

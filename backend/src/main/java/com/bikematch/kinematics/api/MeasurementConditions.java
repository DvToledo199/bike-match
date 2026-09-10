package com.bikematch.kinematics.api;

public record MeasurementConditions(
        double sagPercent,
        int chainringTeeth,
        int sprocketTeeth,
        String modelVersion,
        ReferenceAssumptions reference
) {
}

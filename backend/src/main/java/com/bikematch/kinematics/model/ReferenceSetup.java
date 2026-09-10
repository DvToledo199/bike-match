package com.bikematch.kinematics.model;

import com.bikematch.kinematics.curve.CurveChecks;

public record ReferenceSetup(WheelConfiguration wheels, double centerOfGravityHeightMm) {
    public ReferenceSetup {
        if (wheels == null) throw new IllegalArgumentException("Wheel configuration is required");
        CurveChecks.positiveFinite(centerOfGravityHeightMm, "Centre of gravity height");
    }

    public static ReferenceSetup standard(WheelConfiguration wheels) {
        // A comparison convention, not a measured rider or a universal physiological average.
        return new ReferenceSetup(wheels, 1100);
    }
}

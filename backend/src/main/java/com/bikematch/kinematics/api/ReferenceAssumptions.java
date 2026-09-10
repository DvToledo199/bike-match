package com.bikematch.kinematics.api;

import com.bikematch.kinematics.model.ReferenceSetup;
import com.bikematch.kinematics.model.WheelConfiguration;

public record ReferenceAssumptions(WheelConfiguration wheelConfiguration,
                                   double frontWheelRadiusMm, double rearWheelRadiusMm,
                                   double centerOfGravityHeightMm, double photoRotationDegrees,
                                   String motionModel, String brakeModel, String validationLevel) {
    public static ReferenceAssumptions from(ReferenceSetup setup, double rotationDegrees) {
        return new ReferenceAssumptions(setup.wheels(), setup.wheels().frontRadiusMm(),
                setup.wheels().rearRadiusMm(), setup.centerOfGravityHeightMm(), rotationDegrees,
                "FIXED_FRAME_LOCAL_GROUND", "SWINGARM_FIXED", "ANALYTICAL_REFERENCE");
    }
}

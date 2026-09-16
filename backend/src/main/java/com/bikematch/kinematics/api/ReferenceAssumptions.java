package com.bikematch.kinematics.api;

import com.bikematch.bike.SuspensionLayout;
import com.bikematch.kinematics.model.ReferenceSetup;
import com.bikematch.kinematics.model.WheelConfiguration;

public record ReferenceAssumptions(WheelConfiguration wheelConfiguration,
                                   double frontWheelRadiusMm, double rearWheelRadiusMm,
                                   double centerOfGravityHeightMm, double photoRotationDegrees,
                                   String motionModel, String brakeModel, String validationLevel) {
    public static ReferenceAssumptions from(ReferenceSetup setup, double rotationDegrees) {
        return from(setup, rotationDegrees, SuspensionLayout.SINGLE_PIVOT);
    }

    public static ReferenceAssumptions from(ReferenceSetup setup, double rotationDegrees,
                                            SuspensionLayout layout) {
        return new ReferenceAssumptions(setup.wheels(), setup.wheels().frontRadiusMm(),
                setup.wheels().rearRadiusMm(), setup.centerOfGravityHeightMm(), rotationDegrees,
                "FIXED_FRAME_LOCAL_GROUND",
                layout == SuspensionLayout.HORST_LINK ? "SEATSTAY_FIXED" : "SWINGARM_FIXED",
                "ANALYTICAL_REFERENCE");
    }
}

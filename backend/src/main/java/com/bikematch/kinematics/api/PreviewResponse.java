package com.bikematch.kinematics.api;

import com.bikematch.kinematics.check.TravelCheck;
import com.bikematch.kinematics.curve.KickbackSample;
import com.bikematch.kinematics.curve.LeverageSample;
import com.bikematch.kinematics.descriptor.AxlePathDescriptors;
import com.bikematch.kinematics.descriptor.LeverageDescriptors;
import com.bikematch.kinematics.geometry.Point2D;

import java.util.List;

public record PreviewResponse(
        List<LeverageSample> leverageCurve,
        List<KickbackSample> kickbackCurve,
        List<Point2D> axlePath,
        LeverageDescriptors leverageDescriptors,
        AxlePathDescriptors axlePathDescriptors,
        TravelCheck travelCheck,
        MeasurementConditions conditions
) {
}
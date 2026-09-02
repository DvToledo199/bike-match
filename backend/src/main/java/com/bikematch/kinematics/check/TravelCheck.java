package com.bikematch.kinematics.check;

import com.bikematch.kinematics.geometry.Point2D;

import java.util.List;

/**
 * Coherence check: rear-wheel travel computed from the marked points vs the travel declared
 * by the manufacturer. A gap over ±10% flags likely bad marking or calibration.
 */
public record TravelCheck(
        double calculatedTravelMm,
        double declaredTravelMm,
        double deviationPercent,
        boolean withinTolerance) {

    private static final double TOLERANCE_PERCENT = 10.0;

    public static TravelCheck from(List<Point2D> axlePath, double declaredTravelMm) {
        double calculatedTravelMm = axlePath.get(0).y() - axlePath.get(axlePath.size() - 1).y();
        double deviationPercent = Math.abs(calculatedTravelMm - declaredTravelMm) / declaredTravelMm * 100.0;
        boolean withinTolerance = deviationPercent <= TOLERANCE_PERCENT;
        return new TravelCheck(calculatedTravelMm, declaredTravelMm, deviationPercent, withinTolerance);
    }
}
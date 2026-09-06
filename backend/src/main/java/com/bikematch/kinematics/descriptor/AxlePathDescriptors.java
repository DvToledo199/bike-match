package com.bikematch.kinematics.descriptor;

import com.bikematch.kinematics.geometry.Point2D;
import com.bikematch.kinematics.curve.CurveChecks;

import java.util.List;

/**
 * Descriptors of the rear-axle path: the maximum rearward excursion (mm) and where along
 * the travel it happens.
 *
 * <p>Coordinate convention (from the marked-photo fixtures): x runs along the bike with the
 * rear at smaller x, so "rearward" is the −x direction. The rearward excursion at a step is
 * restX − axleX (0 if the axle never goes behind its rest position). atTravelPercent is the
 * fraction of total vertical travel at which the maximum occurs.
 */
public record AxlePathDescriptors(double maxRearwardMm, double atTravelPercent) {

    public static AxlePathDescriptors from(List<Point2D> axlePath) {
        CurveChecks.compressionPath(axlePath);
        Point2D rest = axlePath.get(0);
        double totalTravelMm = rest.y() - axlePath.get(axlePath.size() - 1).y();

        double maxRearwardMm = 0.0;
        double atTravelPercent = 0.0;
        for (Point2D axle : axlePath) {
            double rearwardMm = rest.x() - axle.x();
            if (rearwardMm > maxRearwardMm) {
                maxRearwardMm = rearwardMm;
                double travelMm = rest.y() - axle.y();
                atTravelPercent = totalTravelMm == 0 ? 0.0 : travelMm / totalTravelMm * 100.0;
            }
        }
        return new AxlePathDescriptors(maxRearwardMm, atTravelPercent);
    }
}

package com.bikematch.kinematics.curve;

import com.bikematch.kinematics.geometry.Point2D;

import java.util.ArrayList;
import java.util.List;

/** The leverage-ratio curve, derived from a solver's axle sweep. */
public record LeverageCurve(List<LeverageSample> samples) {

    public static LeverageCurve from(List<Point2D> axlePath, double shockStrokeMm) {
        double shockStep = shockStrokeMm / (axlePath.size() - 1);
        double restY = axlePath.get(0).y();

        List<LeverageSample> samples = new ArrayList<>();
        for (int i = 1; i < axlePath.size(); i++) {
            double wheelTravel = restY - axlePath.get(i).y();
            double wheelStep = axlePath.get(i - 1).y() - axlePath.get(i).y();
            double ratio = wheelStep / shockStep;
            samples.add(new LeverageSample(wheelTravel, ratio));
        }
        return new LeverageCurve(samples);
    }
}
package com.bikematch.kinematics.curve;

import com.bikematch.kinematics.geometry.Point2D;

import java.util.ArrayList;
import java.util.List;

/**
 * The pedal-kickback curve, derived from a solver's axle sweep.
 *
 * <p>Simplified v1 model: kickback (°) = chain growth / chainring pitch radius, where
 * chain growth is the bottom-bracket-to-axle distance change and the rear wheel is treated
 * as planted. The cog's effect is neglected. Rationale in
 * {@code docs/fundamentos-motor-cinematica.md}; cog-aware refinement tracked in issue #31.
 */
public record KickbackCurve(List<KickbackSample> samples) {

    /** Standard bicycle chain pitch: half an inch, in millimetres. */
    private static final double CHAIN_PITCH_MM = 12.7;

    public static KickbackCurve from(List<Point2D> axlePath, Point2D bottomBracket, int chainringTeeth) {
        double chainringPitchRadius = chainringTeeth * CHAIN_PITCH_MM / (2 * Math.PI);
        double restChainLength = bottomBracket.distanceTo(axlePath.get(0));
        double restY = axlePath.get(0).y();

        List<KickbackSample> samples = new ArrayList<>();
        for (int i = 1; i < axlePath.size(); i++) {
            Point2D axle = axlePath.get(i);
            double wheelTravel = restY - axle.y();
            double chainGrowth = bottomBracket.distanceTo(axle) - restChainLength;
            double kickbackDegrees = Math.toDegrees(chainGrowth / chainringPitchRadius);
            samples.add(new KickbackSample(wheelTravel, kickbackDegrees));
        }
        return new KickbackCurve(samples);
    }
}
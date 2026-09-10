package com.bikematch.kinematics.curve;

import com.bikematch.kinematics.geometry.Point2D;
import com.bikematch.kinematics.geometry.ChainDrive;

import java.util.ArrayList;
import java.util.List;

/**
 * The pedal-kickback curve, derived from a solver's axle sweep.
 *
 * <p>The legacy overload retains the v1 distance-only estimate for requests without wheel data.
 * The cog-aware overload sums upper-span growth, chain wrap and wheel rollback.
 * Physical conventions and sources: {@code docs/modelo-referencia-cinematica.md}.
 */
public record KickbackCurve(List<KickbackSample> samples) {
    public KickbackCurve {
        samples = List.copyOf(samples);
    }

    /** Standard bicycle chain pitch: half an inch, in millimetres. */
    private static final double CHAIN_PITCH_MM = 12.7;

    public static KickbackCurve from(List<Point2D> axlePath, Point2D bottomBracket,
                                     ChainDrive drive, double rearWheelRadiusMm) {
        CurveChecks.compressionPath(axlePath);
        CurveChecks.positiveFinite(rearWheelRadiusMm, "Rear wheel radius");
        if (drive == null) throw new IllegalArgumentException("Chain drive is required");
        Point2D rest = axlePath.getFirst();
        ChainDrive.ChainLine initialChain = drive.at(bottomBracket, rest);
        List<KickbackSample> samples = new ArrayList<>();
        for (Point2D axle : axlePath) {
            ChainDrive.ChainLine chain = drive.at(bottomBracket, axle);
            double spanGrowth = (chain.lengthMm() - initialChain.lengthMm()) / drive.chainringRadiusMm();
            double wrap = (chain.angleRadians() - initialChain.angleRadians()) * (drive.gearRatio() - 1);
            double rollback = (rest.x() - axle.x()) / rearWheelRadiusMm * drive.gearRatio();
            double degrees = Math.toDegrees(spanGrowth + wrap + rollback);
            CurveChecks.finite(degrees, "Pedal kickback");
            samples.add(new KickbackSample(rest.y() - axle.y(), degrees));
        }
        return new KickbackCurve(samples);
    }

    public static KickbackCurve from(List<Point2D> axlePath, Point2D bottomBracket, int chainringTeeth) {
        CurveChecks.compressionPath(axlePath);
        CurveChecks.positiveFinite(chainringTeeth, "Chainring teeth");
        if (bottomBracket == null) throw new IllegalArgumentException("Bottom bracket is required");
        double chainringPitchRadius = chainringTeeth * CHAIN_PITCH_MM / (2 * Math.PI);
        double restChainLength = bottomBracket.distanceTo(axlePath.get(0));
        double restY = axlePath.get(0).y();

        List<KickbackSample> samples = new ArrayList<>();
        for (int i = 1; i < axlePath.size(); i++) {
            Point2D axle = axlePath.get(i);
            double wheelTravel = restY - axle.y();
            double chainGrowth = bottomBracket.distanceTo(axle) - restChainLength;
            double kickbackDegrees = Math.toDegrees(chainGrowth / chainringPitchRadius);
            CurveChecks.finite(kickbackDegrees, "Pedal kickback");
            samples.add(new KickbackSample(wheelTravel, kickbackDegrees));
        }
        return new KickbackCurve(samples);
    }
}

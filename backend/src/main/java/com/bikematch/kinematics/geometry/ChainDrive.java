package com.bikematch.kinematics.geometry;

import com.bikematch.kinematics.curve.CurveChecks;

/** Smooth pitch-circle model of a direct chain, without an idler. Coordinates are x-forward/y-down. */
public record ChainDrive(int chainringTeeth, int sprocketTeeth) {
    public ChainDrive {
        if (chainringTeeth < 3 || sprocketTeeth < 3) {
            throw new IllegalArgumentException("Each sprocket must have at least three teeth");
        }
    }

    public double chainringRadiusMm() { return chainringTeeth * 12.7 / (2 * Math.PI); }
    public double sprocketRadiusMm() { return sprocketTeeth * 12.7 / (2 * Math.PI); }
    public double gearRatio() { return (double) sprocketTeeth / chainringTeeth; }

    public ChainLine at(Point2D bottomBracket, Point2D rearAxle) {
        if (bottomBracket == null || rearAxle == null) {
            throw new IllegalArgumentException("Both chain centres are required");
        }
        double dx = bottomBracket.x() - rearAxle.x();
        double dy = bottomBracket.y() - rearAxle.y();
        CurveChecks.positiveFinite(dx, "Bottom bracket ahead of rear axle");
        CurveChecks.finite(dy, "Chain centre height difference");
        double distance = Math.hypot(dx, dy);
        if (distance <= chainringRadiusMm() + sprocketRadiusMm()) {
            throw new IllegalArgumentException("Chain pitch circles must not overlap");
        }
        double radiusDifference = sprocketRadiusMm() - chainringRadiusMm();
        return new ChainLine(Math.sqrt(distance * distance - radiusDifference * radiusDifference),
                Math.atan2(dy, dx) + Math.asin(radiusDifference / distance));
    }

    /** Upper straight span; angle is clockwise from the forward horizontal. */
    public record ChainLine(double lengthMm, double angleRadians) { }
}

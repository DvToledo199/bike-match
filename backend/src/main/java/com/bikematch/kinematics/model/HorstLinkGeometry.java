package com.bikematch.kinematics.model;

import com.bikematch.kinematics.geometry.Point2D;

import java.util.Objects;

/**
 * Fully extended geometry of the conventional Horst-link layout supported by the first solver.
 *
 * <p>The four-bar loop is mainPivot → horstPivot → rockerSeatstayPivot → rockerFramePivot.
 * The shock joins shockFrame to shockRocker, and the rear axle is rigidly attached to the
 * horstPivot/rockerSeatstayPivot coupler.</p>
 */
public record HorstLinkGeometry(
        Point2D mainPivot,
        Point2D horstPivot,
        Point2D rockerFramePivot,
        Point2D rockerSeatstayPivot,
        Point2D shockFrame,
        Point2D shockRocker,
        Point2D rearAxle) {

    public HorstLinkGeometry {
        requireFinitePoint(mainPivot, "Main pivot");
        requireFinitePoint(horstPivot, "Horst pivot");
        requireFinitePoint(rockerFramePivot, "Rocker-frame pivot");
        requireFinitePoint(rockerSeatstayPivot, "Rocker-seatstay pivot");
        requireFinitePoint(shockFrame, "Shock frame eye");
        requireFinitePoint(shockRocker, "Shock rocker eye");
        requireFinitePoint(rearAxle, "Rear axle");

        requireDistinct(mainPivot, horstPivot, "Main-pivot to Horst-pivot bar");
        requireDistinct(horstPivot, rockerSeatstayPivot, "Seatstay coupler");
        requireDistinct(rockerSeatstayPivot, rockerFramePivot, "Rocker bar");
        requireDistinct(shockRocker, rockerFramePivot, "Rocker pivot to shock eye");
        requireDistinct(shockFrame, shockRocker, "Shock eyes");
    }

    private static void requireFinitePoint(Point2D point, String name) {
        Objects.requireNonNull(point, name + " is required");
        if (!Double.isFinite(point.x()) || !Double.isFinite(point.y())) {
            throw new IllegalArgumentException(name + " must have finite coordinates");
        }
    }

    private static void requireDistinct(Point2D first, Point2D second, String name) {
        if (first.distanceTo(second) <= 1e-9) {
            throw new IllegalArgumentException(name + " must have a non-zero length");
        }
    }
}

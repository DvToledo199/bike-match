package com.bikematch.kinematics.model;

import com.bikematch.kinematics.geometry.Point2D;

import java.util.Objects;

/**
 * Fully extended geometry of a Horst link driven through a rigid yoke.
 *
 * <p>The yoke is fixed relative to the shock axis and pivots at {@code yokeRockerPivot}.
 * Its physical shock eye is not a rigid point of the rocker. The stroke and photo scale use
 * {@code shockFrame} and {@code shockYokeEye}, excluding the extension.</p>
 */
public record HorstLinkYokeGeometry(
        Point2D mainPivot,
        Point2D horstPivot,
        Point2D rockerFramePivot,
        Point2D rockerSeatstayPivot,
        Point2D shockFrame,
        Point2D yokeRockerPivot,
        Point2D shockYokeEye,
        Point2D rearAxle) {

    public HorstLinkYokeGeometry {
        requireFinitePoint(mainPivot, "Main pivot");
        requireFinitePoint(horstPivot, "Horst pivot");
        requireFinitePoint(rockerFramePivot, "Rocker-frame pivot");
        requireFinitePoint(rockerSeatstayPivot, "Rocker-seatstay pivot");
        requireFinitePoint(shockFrame, "Shock frame eye");
        requireFinitePoint(yokeRockerPivot, "Yoke-rocker pivot");
        requireFinitePoint(shockYokeEye, "Shock yoke eye");
        requireFinitePoint(rearAxle, "Rear axle");

        requireDistinct(mainPivot, horstPivot, "Main-pivot to Horst-pivot bar");
        requireDistinct(horstPivot, rockerSeatstayPivot, "Seatstay coupler");
        requireDistinct(rockerSeatstayPivot, rockerFramePivot, "Rocker bar");
        requireDistinct(rockerFramePivot, yokeRockerPivot, "Rocker pivot to yoke joint");
        requireDistinct(yokeRockerPivot, shockYokeEye, "Yoke extension");
        requireDistinct(shockFrame, shockYokeEye, "Shock eyes");
    }

    public HorstLinkGeometry asHorstLinkGeometry() {
        return new HorstLinkGeometry(mainPivot, horstPivot, rockerFramePivot, rockerSeatstayPivot,
                shockFrame, yokeRockerPivot, rearAxle);
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

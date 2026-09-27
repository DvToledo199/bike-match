package com.bikematch.kinematics.model;

import com.bikematch.kinematics.geometry.Point2D;

import java.util.Objects;

/**
 * Fully extended geometry of a Horst link whose shock is driven by the seatstay.
 *
 * <p>The four-bar loop is the conventional one: mainPivot → horstPivot → rockerSeatstayPivot →
 * rockerFramePivot. The rocker only guides the seatstay; the shock joins shockFrame to
 * {@code shockSeatstay}, a rigid point of the axle-carrying seatstay coupler.</p>
 */
public record HorstLinkSeatstayGeometry(
        Point2D mainPivot,
        Point2D horstPivot,
        Point2D rockerFramePivot,
        Point2D rockerSeatstayPivot,
        Point2D shockFrame,
        Point2D shockSeatstay,
        Point2D rearAxle) {

    public HorstLinkSeatstayGeometry {
        requireFinitePoint(mainPivot, "Main pivot");
        requireFinitePoint(horstPivot, "Horst pivot");
        requireFinitePoint(rockerFramePivot, "Rocker-frame pivot");
        requireFinitePoint(rockerSeatstayPivot, "Rocker-seatstay pivot");
        requireFinitePoint(shockFrame, "Shock frame eye");
        requireFinitePoint(shockSeatstay, "Shock seatstay eye");
        requireFinitePoint(rearAxle, "Rear axle");

        requireDistinct(mainPivot, horstPivot, "Main-pivot to Horst-pivot bar");
        requireDistinct(horstPivot, rockerSeatstayPivot, "Seatstay coupler");
        requireDistinct(rockerSeatstayPivot, rockerFramePivot, "Rocker bar");
        requireDistinct(shockFrame, shockSeatstay, "Shock eyes");
    }

    /**
     * The shared Horst-link view used by the reference curves. They read only the four-bar pivots
     * and the axle, so the seatstay shock eye stands in the shock slot.
     */
    public HorstLinkGeometry asHorstLinkGeometry() {
        return new HorstLinkGeometry(mainPivot, horstPivot, rockerFramePivot, rockerSeatstayPivot,
                shockFrame, shockSeatstay, rearAxle);
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

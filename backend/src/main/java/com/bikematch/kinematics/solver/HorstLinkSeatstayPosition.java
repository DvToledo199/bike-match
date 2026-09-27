package com.bikematch.kinematics.solver;

import com.bikematch.kinematics.geometry.Point2D;

/** A solved seatstay-driven Horst-link position that keeps the seatstay shock eye explicit. */
public record HorstLinkSeatstayPosition(
        double shockCompressionMm,
        Point2D horstPivot,
        Point2D rockerSeatstayPivot,
        Point2D shockSeatstay,
        Point2D rearAxle) {

    /** The shared Horst-link view used by the reference curves; the seatstay eye fills the shock slot. */
    public HorstLinkPosition asHorstLinkPosition() {
        return new HorstLinkPosition(
                shockCompressionMm, horstPivot, rockerSeatstayPivot, shockSeatstay, rearAxle);
    }
}

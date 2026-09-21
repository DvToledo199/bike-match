package com.bikematch.kinematics.solver;

import com.bikematch.kinematics.geometry.Point2D;

/** A solved Horst-link position that keeps the rigid yoke points explicit. */
public record HorstLinkYokePosition(
        double shockCompressionMm,
        Point2D horstPivot,
        Point2D rockerSeatstayPivot,
        Point2D yokeRockerPivot,
        Point2D shockYokeEye,
        Point2D rearAxle) {

    public HorstLinkPosition asHorstLinkPosition() {
        return new HorstLinkPosition(
                shockCompressionMm, horstPivot, rockerSeatstayPivot, shockYokeEye, rearAxle);
    }
}

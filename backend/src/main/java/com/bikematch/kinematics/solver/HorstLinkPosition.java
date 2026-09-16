package com.bikematch.kinematics.solver;

import com.bikematch.kinematics.geometry.Point2D;

/** A solved Horst-link position at one shock-compression step. */
public record HorstLinkPosition(
        double shockCompressionMm,
        Point2D horstPivot,
        Point2D rockerSeatstayPivot,
        Point2D shockRocker,
        Point2D rearAxle) {
}

package com.bikematch.kinematics.solver;

import com.bikematch.kinematics.curve.CurveChecks;
import com.bikematch.kinematics.geometry.CircleIntersections;
import com.bikematch.kinematics.geometry.Point2D;
import com.bikematch.kinematics.model.HorstLinkYokeGeometry;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Analytic sweep of a Horst link whose rocker carries a rigid shock yoke.
 *
 * <p>The physical shock eye drives the rocker. The yoke-rocker joint is transported with the
 * same rotation, retaining the complete marked yoke geometry at every position.</p>
 */
public final class HorstLinkYokeSolver {
    private static final int STEPS = 100;

    public List<HorstLinkYokePosition> sweep(HorstLinkYokeGeometry geometry, double shockStrokeMm) {
        Objects.requireNonNull(geometry, "Horst-link yoke geometry is required");
        CurveChecks.positiveFinite(shockStrokeMm, "Shock stroke");

        Point2D mainPivot = geometry.mainPivot();
        Point2D rockerFramePivot = geometry.rockerFramePivot();
        Point2D shockFrame = geometry.shockFrame();
        double chainstayLength = mainPivot.distanceTo(geometry.horstPivot());
        double seatstayLength = geometry.horstPivot().distanceTo(geometry.rockerSeatstayPivot());
        double rockerToShockEyeLength = rockerFramePivot.distanceTo(geometry.shockYokeEye());
        double restShockLength = shockFrame.distanceTo(geometry.shockYokeEye());

        if (shockStrokeMm >= restShockLength) {
            throw new IllegalArgumentException("Shock stroke must be shorter than eye-to-eye length");
        }

        Point2D previousShockEye = geometry.shockYokeEye();
        Point2D previousHorstPivot = geometry.horstPivot();
        List<HorstLinkYokePosition> positions = new ArrayList<>(STEPS + 1);

        for (int step = 0; step <= STEPS; step++) {
            double compression = shockStrokeMm * step / STEPS;
            double shockLength = restShockLength - compression;

            Point2D shockYokeEye = continuousIntersection(
                    CircleIntersections.between(rockerFramePivot, rockerToShockEyeLength,
                            shockFrame, shockLength),
                    previousShockEye, compression, "rigid yoke");
            double rockerRotation = rotationBetweenVectors(rockerFramePivot, geometry.shockYokeEye(),
                    rockerFramePivot, shockYokeEye);
            Point2D rockerSeatstayPivot = geometry.rockerSeatstayPivot()
                    .rotateAround(rockerFramePivot, rockerRotation);
            Point2D yokeRockerPivot = geometry.yokeRockerPivot()
                    .rotateAround(rockerFramePivot, rockerRotation);

            Point2D horstPivot = continuousIntersection(
                    CircleIntersections.between(mainPivot, chainstayLength,
                            rockerSeatstayPivot, seatstayLength),
                    previousHorstPivot, compression, "four-bar");
            Point2D rearAxle = transportWithSeatstay(geometry, horstPivot, rockerSeatstayPivot);

            positions.add(new HorstLinkYokePosition(compression, horstPivot, rockerSeatstayPivot,
                    yokeRockerPivot, shockYokeEye, rearAxle));
            previousShockEye = shockYokeEye;
            previousHorstPivot = horstPivot;
        }
        return List.copyOf(positions);
    }

    private Point2D continuousIntersection(List<Point2D> candidates, Point2D previous,
                                           double compression, String mechanism) {
        if (candidates.size() != 2) {
            throw new IllegalArgumentException("The " + mechanism + " linkage reaches an impossible or "
                    + "singular position at " + compression + " mm of shock compression");
        }
        Point2D first = candidates.getFirst();
        Point2D second = candidates.getLast();
        return first.distanceTo(previous) <= second.distanceTo(previous) ? first : second;
    }

    private double rotationBetweenVectors(Point2D restStart, Point2D restEnd,
                                          Point2D currentStart, Point2D currentEnd) {
        double restX = restEnd.x() - restStart.x();
        double restY = restEnd.y() - restStart.y();
        double currentX = currentEnd.x() - currentStart.x();
        double currentY = currentEnd.y() - currentStart.y();
        return Math.atan2(restX * currentY - restY * currentX,
                restX * currentX + restY * currentY);
    }

    private Point2D transportWithSeatstay(HorstLinkYokeGeometry geometry, Point2D horstPivot,
                                          Point2D rockerSeatstayPivot) {
        Point2D restHorstPivot = geometry.horstPivot();
        Point2D restRockerSeatstayPivot = geometry.rockerSeatstayPivot();
        double rotation = rotationBetweenVectors(restHorstPivot, restRockerSeatstayPivot,
                horstPivot, rockerSeatstayPivot);
        Point2D rotatedRearAxle = geometry.rearAxle().rotateAround(restHorstPivot, rotation);
        return new Point2D(rotatedRearAxle.x() - restHorstPivot.x() + horstPivot.x(),
                rotatedRearAxle.y() - restHorstPivot.y() + horstPivot.y());
    }
}

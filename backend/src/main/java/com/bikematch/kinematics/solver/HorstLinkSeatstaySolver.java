package com.bikematch.kinematics.solver;

import com.bikematch.kinematics.curve.CurveChecks;
import com.bikematch.kinematics.geometry.CircleIntersections;
import com.bikematch.kinematics.geometry.Point2D;
import com.bikematch.kinematics.model.HorstLinkSeatstayGeometry;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Sweep of a Horst link whose shock is driven by the seatstay coupler instead of the rocker.
 *
 * <p>Here the shock length depends on the whole four-bar, so one circle intersection cannot place
 * the linkage from the shock length. The sweep uses the rocker angle as its input instead: at any
 * trial angle the four-bar is solved analytically, exactly as in the direct Horst link, and the
 * shock is measured to its eye on the seatstay. For each compression step the angle is bracketed
 * with small forward steps and then narrowed by bisection until the shock has the requested
 * length. The bracket always follows the previous step, so the linkage cannot jump to another
 * assembly.</p>
 */
public final class HorstLinkSeatstaySolver {
    private static final int STEPS = 100;
    private static final double BRACKET_STEP_RADIANS = Math.toRadians(0.5);
    private static final double MAXIMUM_ROCKER_TURN_RADIANS = Math.toRadians(120);
    private static final int BISECTIONS = 60;

    public List<HorstLinkSeatstayPosition> sweep(HorstLinkSeatstayGeometry geometry, double shockStrokeMm) {
        Objects.requireNonNull(geometry, "Seatstay-driven Horst-link geometry is required");
        CurveChecks.positiveFinite(shockStrokeMm, "Shock stroke");
        double restShockLength = geometry.shockFrame().distanceTo(geometry.shockSeatstay());
        if (shockStrokeMm >= restShockLength) {
            throw new IllegalArgumentException("Shock stroke must be shorter than eye-to-eye length");
        }

        Linkage linkage = new Linkage(geometry);
        Pose previous = linkage.at(0, geometry.horstPivot());
        if (previous == null) {
            throw impossiblePosition(0);
        }
        double direction = compressingDirection(linkage, previous);
        List<HorstLinkSeatstayPosition> positions = new ArrayList<>(STEPS + 1);
        positions.add(previous.asPosition(0));

        for (int step = 1; step <= STEPS; step++) {
            double compression = shockStrokeMm * step / STEPS;
            previous = solve(linkage, previous, direction, restShockLength - compression, compression);
            positions.add(previous.asPosition(compression));
        }
        return List.copyOf(positions);
    }

    /** The rocker turn that shortens the shock: the one a compressing seatstay follows. */
    private double compressingDirection(Linkage linkage, Pose rest) {
        double direction = 0;
        double shortest = rest.shockLength();
        for (double sign : new double[] {1, -1}) {
            Pose trial = linkage.at(sign * BRACKET_STEP_RADIANS, rest.horstPivot());
            if (trial != null && trial.shockLength() < shortest) {
                shortest = trial.shockLength();
                direction = sign;
            }
        }
        if (direction == 0) {
            throw impossiblePosition(0);
        }
        return direction;
    }

    private Pose solve(Linkage linkage, Pose previous, double direction, double targetLength, double compression) {
        Pose longer = previous;
        while (true) {
            double angle = longer.rockerAngle() + direction * BRACKET_STEP_RADIANS;
            if (Math.abs(angle) > MAXIMUM_ROCKER_TURN_RADIANS) {
                throw impossiblePosition(compression);
            }
            Pose next = linkage.at(angle, longer.horstPivot());
            if (next == null) {
                throw impossiblePosition(compression);
            }
            if (next.shockLength() <= targetLength) {
                return bisect(linkage, longer, next, targetLength, compression);
            }
            longer = next;
        }
    }

    private Pose bisect(Linkage linkage, Pose longer, Pose shorter, double targetLength, double compression) {
        for (int i = 0; i < BISECTIONS; i++) {
            Pose middle = linkage.at((longer.rockerAngle() + shorter.rockerAngle()) / 2, longer.horstPivot());
            if (middle == null) {
                throw impossiblePosition(compression);
            }
            if (middle.shockLength() > targetLength) {
                longer = middle;
            } else {
                shorter = middle;
            }
        }
        return Math.abs(longer.shockLength() - targetLength) <= Math.abs(shorter.shockLength() - targetLength)
                ? longer : shorter;
    }

    private IllegalArgumentException impossiblePosition(double compression) {
        return new IllegalArgumentException("The seatstay linkage reaches an impossible or singular position at "
                + compression + " mm of shock compression");
    }

    /** The four-bar with its fixed bar lengths, solved for any rocker angle. */
    private record Linkage(HorstLinkSeatstayGeometry geometry, double chainstayLength, double seatstayLength) {

        Linkage(HorstLinkSeatstayGeometry geometry) {
            this(geometry, geometry.mainPivot().distanceTo(geometry.horstPivot()),
                    geometry.horstPivot().distanceTo(geometry.rockerSeatstayPivot()));
        }

        /** The linkage with the rocker turned by {@code angle}, or null if it cannot be assembled. */
        Pose at(double angle, Point2D previousHorstPivot) {
            Point2D rockerSeatstayPivot = geometry.rockerSeatstayPivot()
                    .rotateAround(geometry.rockerFramePivot(), angle);
            List<Point2D> candidates = CircleIntersections.between(geometry.mainPivot(), chainstayLength,
                    rockerSeatstayPivot, seatstayLength);
            if (candidates.size() != 2) {
                return null;
            }
            Point2D first = candidates.getFirst();
            Point2D second = candidates.getLast();
            Point2D horstPivot = first.distanceTo(previousHorstPivot) <= second.distanceTo(previousHorstPivot)
                    ? first : second;

            Point2D restHorstPivot = geometry.horstPivot();
            double seatstayRotation = rotationBetweenVectors(restHorstPivot, geometry.rockerSeatstayPivot(),
                    horstPivot, rockerSeatstayPivot);
            Point2D shockSeatstay = transport(geometry.shockSeatstay(), restHorstPivot, horstPivot, seatstayRotation);
            Point2D rearAxle = transport(geometry.rearAxle(), restHorstPivot, horstPivot, seatstayRotation);
            return new Pose(angle, horstPivot, rockerSeatstayPivot, shockSeatstay, rearAxle,
                    geometry.shockFrame().distanceTo(shockSeatstay));
        }

        /** Moves a rigid point of the seatstay with it: turn about the Horst pivot, then follow it. */
        private static Point2D transport(Point2D point, Point2D restHorstPivot, Point2D horstPivot, double rotation) {
            Point2D rotated = point.rotateAround(restHorstPivot, rotation);
            return new Point2D(rotated.x() - restHorstPivot.x() + horstPivot.x(),
                    rotated.y() - restHorstPivot.y() + horstPivot.y());
        }

        private static double rotationBetweenVectors(Point2D restStart, Point2D restEnd,
                                                     Point2D currentStart, Point2D currentEnd) {
            double restX = restEnd.x() - restStart.x();
            double restY = restEnd.y() - restStart.y();
            double currentX = currentEnd.x() - currentStart.x();
            double currentY = currentEnd.y() - currentStart.y();
            return Math.atan2(restX * currentY - restY * currentX,
                    restX * currentX + restY * currentY);
        }
    }

    private record Pose(double rockerAngle, Point2D horstPivot, Point2D rockerSeatstayPivot,
                        Point2D shockSeatstay, Point2D rearAxle, double shockLength) {

        HorstLinkSeatstayPosition asPosition(double compression) {
            return new HorstLinkSeatstayPosition(compression, horstPivot, rockerSeatstayPivot,
                    shockSeatstay, rearAxle);
        }
    }
}

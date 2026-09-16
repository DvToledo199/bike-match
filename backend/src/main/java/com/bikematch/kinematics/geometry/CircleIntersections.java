package com.bikematch.kinematics.geometry;

import java.util.List;
import java.util.Objects;

/**
 * Analytic intersection of two circles in the kinematics plane.
 *
 * <p>The returned positions are ordered consistently around the line from the first centre to the
 * second: the first position is on its positive perpendicular side. A caller that models a
 * linkage can therefore select and keep the same assembly branch while it sweeps.</p>
 */
public final class CircleIntersections {
    private static final double RELATIVE_TOLERANCE = 1e-10;

    private CircleIntersections() {
    }

    /**
     * Returns zero, one or two positions where the two circles meet.
     *
     * @throws IllegalArgumentException when an input is not finite, a radius is not positive, or the
     *                                  circles coincide and consequently have no unique solution
     */
    public static List<Point2D> between(Point2D firstCenter, double firstRadius,
                                        Point2D secondCenter, double secondRadius) {
        requireFinitePoint(firstCenter, "First circle centre");
        requireFinitePoint(secondCenter, "Second circle centre");
        requireRadius(firstRadius, "First circle radius");
        requireRadius(secondRadius, "Second circle radius");

        double deltaX = secondCenter.x() - firstCenter.x();
        double deltaY = secondCenter.y() - firstCenter.y();
        double centreDistance = Math.hypot(deltaX, deltaY);
        double tolerance = toleranceFor(centreDistance, firstRadius, secondRadius);

        if (centreDistance <= tolerance) {
            if (Math.abs(firstRadius - secondRadius) <= tolerance) {
                throw new IllegalArgumentException("Coincident circles have infinitely many intersections");
            }
            return List.of();
        }

        if (centreDistance > firstRadius + secondRadius + tolerance
                || centreDistance < Math.abs(firstRadius - secondRadius) - tolerance) {
            return List.of();
        }

        double distanceAlongCentreLine = (firstRadius * firstRadius - secondRadius * secondRadius
                + centreDistance * centreDistance) / (2 * centreDistance);
        double heightSquared = firstRadius * firstRadius
                - distanceAlongCentreLine * distanceAlongCentreLine;

        if (heightSquared < -tolerance * tolerance) {
            return List.of();
        }

        double height = Math.sqrt(Math.max(0, heightSquared));
        double baseX = firstCenter.x() + distanceAlongCentreLine * deltaX / centreDistance;
        double baseY = firstCenter.y() + distanceAlongCentreLine * deltaY / centreDistance;
        double perpendicularX = -deltaY / centreDistance;
        double perpendicularY = deltaX / centreDistance;

        Point2D firstPosition = new Point2D(baseX + height * perpendicularX,
                baseY + height * perpendicularY);
        if (height <= tolerance) {
            return List.of(firstPosition);
        }
        Point2D secondPosition = new Point2D(baseX - height * perpendicularX,
                baseY - height * perpendicularY);
        return List.of(firstPosition, secondPosition);
    }

    private static void requireFinitePoint(Point2D point, String name) {
        Objects.requireNonNull(point, name + " is required");
        if (!Double.isFinite(point.x()) || !Double.isFinite(point.y())) {
            throw new IllegalArgumentException(name + " must have finite coordinates");
        }
    }

    private static void requireRadius(double radius, String name) {
        if (!Double.isFinite(radius) || radius <= 0) {
            throw new IllegalArgumentException(name + " must be a finite positive number");
        }
    }

    private static double toleranceFor(double centreDistance, double firstRadius, double secondRadius) {
        return RELATIVE_TOLERANCE * Math.max(1.0,
                Math.max(centreDistance, Math.max(firstRadius, secondRadius)));
    }
}

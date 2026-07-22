package com.bikematch.kinematics;

/**
 * x and y are expressed in millimeters (mm)
 */
public record Point2D(double x, double y) {
    public double distanceTo(Point2D other) {
        double deltaX = other.x() - x;
        double deltaY = other.y() - y;
        return Math.sqrt(deltaX * deltaX + deltaY * deltaY);
    }

    public Point2D rotateAround(Point2D center, double angle) {
        double dx = x - center.x();
        double dy = y - center.y();
        double rotatedX = dx * Math.cos(angle) - dy * Math.sin(angle);
        double rotatedY = dx * Math.sin(angle) + dy * Math.cos(angle);
        return new Point2D(center.x() + rotatedX, center.y() + rotatedY);
    }
}

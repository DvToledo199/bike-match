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
}

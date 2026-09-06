package com.bikematch.kinematics.curve;

import com.bikematch.kinematics.geometry.Point2D;
import java.util.List;

/** Shared guards at the boundary between the geometric sweep and derived curves. */
public final class CurveChecks {
    private CurveChecks() { }

    public static void positiveFinite(double value, String name) {
        if (!Double.isFinite(value) || value <= 0) {
            throw new IllegalArgumentException(name + " must be a finite positive number");
        }
    }

    public static void finite(double value, String name) {
        if (!Double.isFinite(value)) {
            throw new IllegalArgumentException(name + " cannot be calculated from these measurements");
        }
    }

    public static void compressionPath(List<Point2D> path) {
        if (path == null || path.size() < 2) {
            throw new IllegalArgumentException("An axle path needs at least two positions");
        }
        for (int i = 0; i < path.size(); i++) {
            Point2D point = path.get(i);
            if (point == null) throw new IllegalArgumentException("An axle position is missing");
            finite(point.x(), "Axle X");
            finite(point.y(), "Axle Y");
            if (i > 0) positiveFinite(path.get(i - 1).y() - point.y(), "Upward wheel movement");
        }
    }
}

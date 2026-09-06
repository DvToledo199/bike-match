package com.bikematch.kinematics.solver;

import com.bikematch.kinematics.geometry.Point2D;
import com.bikematch.kinematics.curve.CurveChecks;
import com.bikematch.kinematics.model.KinematicsInput;
import com.bikematch.kinematics.model.PointType;

import java.util.ArrayList;
import java.util.List;

public class MonopivotSolver {

    /** Beyond this overshoot past [-1, 1], the triangle is impossible (bad marking), not FP rounding noise. */
    private static final double COS_TOLERANCE = 0.001;

    public double swingarmLength(KinematicsInput input) {
        Point2D pivot = input.pointOf(PointType.MAIN_PIVOT);
        Point2D axle = input.pointOf(PointType.REAR_AXLE);
        return pivot.distanceTo(axle);
    }

    public double pivotAngle(double pivotToFrame, double pivotToSwingarm, double shockLength) {
        CurveChecks.positiveFinite(pivotToFrame, "Pivot-to-frame distance");
        CurveChecks.positiveFinite(pivotToSwingarm, "Pivot-to-swingarm distance");
        CurveChecks.positiveFinite(shockLength, "Shock length");
        double a = pivotToFrame;
        double b = pivotToSwingarm;
        double c = shockLength;
        double cos = (a * a + b * b - c * c) / (2 * a * b);
        CurveChecks.finite(cos, "Pivot angle");

        if (cos > 1 + COS_TOLERANCE || cos < -1 - COS_TOLERANCE) {
            throw new IllegalArgumentException(
                    "Impossible pivot geometry (cos=" + cos + "): the marked points can't form this shock length");
        }

        double clamped = Math.max(-1, Math.min(1, cos));
        return Math.acos(clamped);
    }

    public List<Point2D> sweep(KinematicsInput input) {
        Point2D pivot = input.pointOf(PointType.MAIN_PIVOT);
        Point2D axle = input.pointOf(PointType.REAR_AXLE);
        Point2D frameAnchor = input.pointOf(PointType.SHOCK_FRAME);
        Point2D swingarmAnchor = input.pointOf(PointType.SHOCK_SWINGARM);

        double pivotToFrame = pivot.distanceTo(frameAnchor);
        double pivotToSwingarm = pivot.distanceTo(swingarmAnchor);
        double restShockLength = frameAnchor.distanceTo(swingarmAnchor);
        double restAngle = pivotAngle(pivotToFrame, pivotToSwingarm, restShockLength);

        double stroke = input.parameters().shockStrokeMm();
        CurveChecks.positiveFinite(stroke, "Shock stroke");
        if (stroke >= restShockLength) {
            throw new IllegalArgumentException("Shock stroke must be shorter than eye-to-eye length");
        }
        int steps = 100;

        List<Point2D> axlePath = new ArrayList<>();
        for (int i = 0; i <= steps; i++) {
            double compression = stroke * i / steps;
            double shockLength = restShockLength - compression;
            double angle = pivotAngle(pivotToFrame, pivotToSwingarm, shockLength);
            // Compression closes the pivot angle, so the swingarm turns the opposite way.
            double rotation = restAngle - angle;
            axlePath.add(axle.rotateAround(pivot, rotation));
        }
        return axlePath;
    }
}

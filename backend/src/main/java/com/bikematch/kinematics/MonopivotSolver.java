package com.bikematch.kinematics;

import java.util.ArrayList;
import java.util.List;

public class MonopivotSolver {
    public double swingarmLength(KinematicsInput input) {

        Point2D pivot = input.pointOf(PointType.MAIN_PIVOT);
        Point2D axle = input.pointOf(PointType.REAR_AXLE);
        return pivot.distanceTo(axle);

    }
    public double pivotAngle(double pivotToFrame, double pivotToSwingarm, double shockLength) {
        double a = pivotToFrame;
        double b = pivotToSwingarm;
        double c = shockLength;
        double cos = (a * a + b * b - c * c) / (2 * a * b);
        return Math.acos(cos);
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
        int steps = 100;

        List<Point2D> axlePath = new ArrayList<>();
        for (int i = 0; i <= steps; i++) {
            double compression = stroke * i / steps;
            double shockLength = restShockLength - compression;
            double angle = pivotAngle(pivotToFrame, pivotToSwingarm, shockLength);
            double rotation = angle - restAngle;
            axlePath.add(axle.rotateAround(pivot, rotation));
        }
        return axlePath;
    }
}

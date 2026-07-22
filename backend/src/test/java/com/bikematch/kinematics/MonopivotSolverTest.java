package com.bikematch.kinematics;

import org.junit.jupiter.api.Test;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class MonopivotSolverTest {

    @Test
    void swingarmLengthIsTheDistanceFromPivotToRearAxle() {
        List<MarkedPoint> points = List.of(
                new MarkedPoint(PointType.MAIN_PIVOT, new Point2D(0, 0)),
                new MarkedPoint(PointType.REAR_AXLE, new Point2D(300, 400))
        );
        KinematicsInput input = new KinematicsInput(points, anyParameters());

        assertEquals(500.0, new MonopivotSolver().swingarmLength(input));
    }

    private KinematicsParameters anyParameters() {
        return new KinematicsParameters(65, 32, 51, 160, 30);
    }
    @Test
    void pivotAngleOfAThreeFourFiveTriangleIsNinetyDegrees() {
        MonopivotSolver solver = new MonopivotSolver();
        double angleInDegrees = Math.toDegrees(solver.pivotAngle(3, 4, 5));
        assertEquals(90.0, angleInDegrees, 0.0001);
    }
    @Test
    void sweepReturnsOnePointForEachStepPlusTheStart() {
        List<Point2D> path = new MonopivotSolver().sweep(monopivotInput());
        assertEquals(101, path.size());
    }

    @Test
    void sweepStartsAtTheRestAxlePosition() {
        List<Point2D> path = new MonopivotSolver().sweep(monopivotInput());
        assertEquals(new Point2D(400, 0), path.get(0));
    }

    private KinematicsInput monopivotInput() {
        List<MarkedPoint> points = List.of(
                new MarkedPoint(PointType.MAIN_PIVOT, new Point2D(0, 0)),
                new MarkedPoint(PointType.REAR_AXLE, new Point2D(400, 0)),
                new MarkedPoint(PointType.SHOCK_FRAME, new Point2D(0, 200)),
                new MarkedPoint(PointType.SHOCK_SWINGARM, new Point2D(150, 150))
        );
        return new KinematicsInput(points, anyParameters());
    }
}
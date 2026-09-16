package com.bikematch.kinematics.solver;

import com.bikematch.kinematics.geometry.Point2D;
import com.bikematch.kinematics.model.HorstLinkGeometry;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class HorstLinkSolverTest {
    private static final double DELTA = 1e-8;
    private final HorstLinkSolver solver = new HorstLinkSolver();

    @Test
    void sweepStartsAtTheMarkedRestPositionAndHasOneHundredSteps() {
        List<HorstLinkPosition> positions = solver.sweep(referenceGeometry(), 10);

        assertEquals(101, positions.size());
        HorstLinkPosition rest = positions.getFirst();
        assertEquals(0, rest.shockCompressionMm(), DELTA);
        assertPoint(rest.horstPivot(), -100, 0);
        assertPoint(rest.rockerSeatstayPivot(), -100, -120);
        assertPoint(rest.shockRocker(), -40, -120);
        assertPoint(rest.rearAxle(), -160, -90);
    }

    @Test
    void sweepKeepsAllRigidBarLengthsAndTheRearAxleMounting() {
        HorstLinkGeometry geometry = referenceGeometry();
        double chainstayLength = geometry.mainPivot().distanceTo(geometry.horstPivot());
        double seatstayLength = geometry.horstPivot().distanceTo(geometry.rockerSeatstayPivot());
        double rockerLength = geometry.rockerFramePivot().distanceTo(geometry.rockerSeatstayPivot());
        double axleToHorst = geometry.rearAxle().distanceTo(geometry.horstPivot());
        double axleToRocker = geometry.rearAxle().distanceTo(geometry.rockerSeatstayPivot());

        for (HorstLinkPosition position : solver.sweep(geometry, 10)) {
            assertEquals(chainstayLength, geometry.mainPivot().distanceTo(position.horstPivot()), DELTA);
            assertEquals(seatstayLength, position.horstPivot().distanceTo(position.rockerSeatstayPivot()), DELTA);
            assertEquals(rockerLength, geometry.rockerFramePivot().distanceTo(position.rockerSeatstayPivot()), DELTA);
            assertEquals(axleToHorst, position.rearAxle().distanceTo(position.horstPivot()), DELTA);
            assertEquals(axleToRocker, position.rearAxle().distanceTo(position.rockerSeatstayPivot()), DELTA);
        }
    }

    @Test
    void sweepFollowsOneAssemblyBranchWithoutLargeStepJumps() {
        List<HorstLinkPosition> positions = solver.sweep(referenceGeometry(), 10);

        for (int index = 1; index < positions.size(); index++) {
            double stepDistance = positions.get(index - 1).rearAxle().distanceTo(positions.get(index).rearAxle());
            assertTrue(stepDistance < 5, "The solver must not switch to the other assembly branch");
        }
    }

    @Test
    void rejectsAStrokeThatCannotReachTheRequestedCompression() {
        assertThrows(IllegalArgumentException.class, () -> solver.sweep(referenceGeometry(), 80));
    }

    @Test
    void rejectsACollapsedLinkBeforeStartingTheSweep() {
        assertThrows(IllegalArgumentException.class, () -> new HorstLinkGeometry(
                new Point2D(0, 0),
                new Point2D(0, 0),
                new Point2D(0, -120),
                new Point2D(-100, -120),
                new Point2D(50, -80),
                new Point2D(-40, -120),
                new Point2D(-160, -90)));
    }

    private HorstLinkGeometry referenceGeometry() {
        return new HorstLinkGeometry(
                new Point2D(0, 0),
                new Point2D(-100, 0),
                new Point2D(0, -120),
                new Point2D(-100, -120),
                new Point2D(50, -80),
                new Point2D(-40, -120),
                new Point2D(-160, -90));
    }

    private void assertPoint(Point2D point, double expectedX, double expectedY) {
        assertEquals(expectedX, point.x(), DELTA);
        assertEquals(expectedY, point.y(), DELTA);
    }
}

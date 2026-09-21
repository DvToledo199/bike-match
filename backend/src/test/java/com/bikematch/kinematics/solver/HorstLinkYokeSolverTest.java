package com.bikematch.kinematics.solver;

import com.bikematch.kinematics.geometry.Point2D;
import com.bikematch.kinematics.model.HorstLinkYokeGeometry;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class HorstLinkYokeSolverTest {
    private static final double DELTA = 1e-8;
    private final HorstLinkYokeSolver solver = new HorstLinkYokeSolver();

    @Test
    void sweepStartsAtTheMarkedRestPositionAndHasOneHundredSteps() {
        List<HorstLinkYokePosition> positions = solver.sweep(referenceGeometry(), 10);

        assertEquals(101, positions.size());
        HorstLinkYokePosition rest = positions.getFirst();
        assertEquals(0, rest.shockCompressionMm(), DELTA);
        assertPoint(rest.horstPivot(), -100, 0);
        assertPoint(rest.rockerSeatstayPivot(), -100, -120);
        assertPoint(rest.yokeRockerPivot(), -60, -90);
        assertPoint(rest.shockYokeEye(), -40, -120);
        assertPoint(rest.rearAxle(), -160, -90);
    }

    @Test
    void sweepKeepsTheFourBarYokeAndShockLengthsRigid() {
        HorstLinkYokeGeometry geometry = referenceGeometry();
        double chainstayLength = geometry.mainPivot().distanceTo(geometry.horstPivot());
        double seatstayLength = geometry.horstPivot().distanceTo(geometry.rockerSeatstayPivot());
        double rockerLength = geometry.rockerFramePivot().distanceTo(geometry.rockerSeatstayPivot());
        double rockerToYokeLength = geometry.rockerFramePivot().distanceTo(geometry.yokeRockerPivot());
        double yokeLength = geometry.yokeRockerPivot().distanceTo(geometry.shockYokeEye());
        double axleToHorst = geometry.rearAxle().distanceTo(geometry.horstPivot());
        double axleToRocker = geometry.rearAxle().distanceTo(geometry.rockerSeatstayPivot());
        double restShockLength = geometry.shockFrame().distanceTo(geometry.shockYokeEye());

        for (HorstLinkYokePosition position : solver.sweep(geometry, 10)) {
            assertEquals(chainstayLength, geometry.mainPivot().distanceTo(position.horstPivot()), DELTA);
            assertEquals(seatstayLength, position.horstPivot().distanceTo(position.rockerSeatstayPivot()), DELTA);
            assertEquals(rockerLength, geometry.rockerFramePivot().distanceTo(position.rockerSeatstayPivot()), DELTA);
            assertEquals(rockerToYokeLength,
                    geometry.rockerFramePivot().distanceTo(position.yokeRockerPivot()), DELTA);
            assertEquals(yokeLength, position.yokeRockerPivot().distanceTo(position.shockYokeEye()), DELTA);
            assertEquals(restShockLength - position.shockCompressionMm(),
                    geometry.shockFrame().distanceTo(position.shockYokeEye()), DELTA);
            assertEquals(axleToHorst, position.rearAxle().distanceTo(position.horstPivot()), DELTA);
            assertEquals(axleToRocker, position.rearAxle().distanceTo(position.rockerSeatstayPivot()), DELTA);
        }
    }

    @Test
    void sweepFollowsOneAssemblyBranchWithoutLargeStepJumps() {
        List<HorstLinkYokePosition> positions = solver.sweep(referenceGeometry(), 10);

        for (int index = 1; index < positions.size(); index++) {
            double stepDistance = positions.get(index - 1).rearAxle().distanceTo(positions.get(index).rearAxle());
            assertTrue(stepDistance < 5, "The solver must not switch to the other assembly branch");
        }
    }

    @Test
    void rejectsACollapsedYokeBeforeStartingTheSweep() {
        assertThrows(IllegalArgumentException.class, () -> new HorstLinkYokeGeometry(
                new Point2D(0, 0),
                new Point2D(-100, 0),
                new Point2D(0, -120),
                new Point2D(-100, -120),
                new Point2D(50, -80),
                new Point2D(-40, -120),
                new Point2D(-40, -120),
                new Point2D(-160, -90)));
    }

    private HorstLinkYokeGeometry referenceGeometry() {
        return new HorstLinkYokeGeometry(
                new Point2D(0, 0),
                new Point2D(-100, 0),
                new Point2D(0, -120),
                new Point2D(-100, -120),
                new Point2D(50, -80),
                new Point2D(-60, -90),
                new Point2D(-40, -120),
                new Point2D(-160, -90));
    }

    private void assertPoint(Point2D point, double expectedX, double expectedY) {
        assertEquals(expectedX, point.x(), DELTA);
        assertEquals(expectedY, point.y(), DELTA);
    }
}

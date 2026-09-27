package com.bikematch.kinematics.solver;

import com.bikematch.kinematics.geometry.Point2D;
import com.bikematch.kinematics.model.HorstLinkGeometry;
import com.bikematch.kinematics.model.HorstLinkSeatstayGeometry;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class HorstLinkSeatstaySolverTest {
    private static final double DELTA = 1e-8;
    private final HorstLinkSeatstaySolver solver = new HorstLinkSeatstaySolver();

    @Test
    void matchesTheRockerDrivenSweepWhenTheShockMeetsTheRockerSeatstayPivot() {
        // That pivot belongs to both the rocker and the seatstay, so both models describe the same linkage.
        Point2D sharedPivot = new Point2D(-100, -120);
        List<HorstLinkPosition> expected = new HorstLinkSolver().sweep(new HorstLinkGeometry(
                new Point2D(0, 0), new Point2D(-100, 0), new Point2D(0, -120), sharedPivot,
                new Point2D(50, -80), sharedPivot, new Point2D(-160, -90)), 10);
        List<HorstLinkSeatstayPosition> actual = solver.sweep(new HorstLinkSeatstayGeometry(
                new Point2D(0, 0), new Point2D(-100, 0), new Point2D(0, -120), sharedPivot,
                new Point2D(50, -80), sharedPivot, new Point2D(-160, -90)), 10);

        assertEquals(expected.size(), actual.size());
        for (int index = 0; index < expected.size(); index++) {
            assertEquals(expected.get(index).shockCompressionMm(), actual.get(index).shockCompressionMm(), DELTA);
            assertSamePoint(expected.get(index).horstPivot(), actual.get(index).horstPivot());
            assertSamePoint(expected.get(index).rockerSeatstayPivot(), actual.get(index).rockerSeatstayPivot());
            assertSamePoint(expected.get(index).rearAxle(), actual.get(index).rearAxle());
        }
    }

    @Test
    void placesEveryStepAtTheRequestedShockLengthWithRigidBars() {
        HorstLinkSeatstayGeometry geometry = seatstayDrivenGeometry();
        double restShockLength = geometry.shockFrame().distanceTo(geometry.shockSeatstay());
        double chainstayLength = geometry.mainPivot().distanceTo(geometry.horstPivot());
        double rockerLength = geometry.rockerFramePivot().distanceTo(geometry.rockerSeatstayPivot());
        double eyeToHorst = geometry.shockSeatstay().distanceTo(geometry.horstPivot());
        double eyeToRocker = geometry.shockSeatstay().distanceTo(geometry.rockerSeatstayPivot());
        double axleToHorst = geometry.rearAxle().distanceTo(geometry.horstPivot());
        double axleToRocker = geometry.rearAxle().distanceTo(geometry.rockerSeatstayPivot());

        List<HorstLinkSeatstayPosition> positions = solver.sweep(geometry, 20);

        assertEquals(101, positions.size());
        for (int index = 0; index < positions.size(); index++) {
            HorstLinkSeatstayPosition position = positions.get(index);
            assertEquals(20.0 * index / 100, position.shockCompressionMm(), DELTA);
            assertEquals(restShockLength - position.shockCompressionMm(),
                    geometry.shockFrame().distanceTo(position.shockSeatstay()), DELTA);
            assertEquals(chainstayLength, geometry.mainPivot().distanceTo(position.horstPivot()), DELTA);
            assertEquals(rockerLength, geometry.rockerFramePivot().distanceTo(position.rockerSeatstayPivot()), DELTA);
            assertEquals(eyeToHorst, position.shockSeatstay().distanceTo(position.horstPivot()), DELTA);
            assertEquals(eyeToRocker, position.shockSeatstay().distanceTo(position.rockerSeatstayPivot()), DELTA);
            assertEquals(axleToHorst, position.rearAxle().distanceTo(position.horstPivot()), DELTA);
            assertEquals(axleToRocker, position.rearAxle().distanceTo(position.rockerSeatstayPivot()), DELTA);
        }
    }

    @Test
    void startsAtTheMarkedRestPositionAndFollowsOneAssemblyBranch() {
        HorstLinkSeatstayGeometry geometry = seatstayDrivenGeometry();
        List<HorstLinkSeatstayPosition> positions = solver.sweep(geometry, 20);

        assertSamePoint(geometry.horstPivot(), positions.getFirst().horstPivot());
        assertSamePoint(geometry.rearAxle(), positions.getFirst().rearAxle());
        for (int index = 1; index < positions.size(); index++) {
            double stepDistance = positions.get(index - 1).rearAxle().distanceTo(positions.get(index).rearAxle());
            assertTrue(stepDistance < 5, "The solver must not switch to the other assembly branch");
        }
    }

    @Test
    void rejectsAStrokeTheSeatstayCannotReach() {
        IllegalArgumentException error = assertThrows(IllegalArgumentException.class,
                () -> solver.sweep(seatstayDrivenGeometry(), 150));

        assertTrue(error.getMessage().startsWith("The seatstay linkage reaches an impossible or singular position"));
    }

    @Test
    void rejectsAStrokeAsLongAsTheShock() {
        IllegalArgumentException error = assertThrows(IllegalArgumentException.class,
                () -> solver.sweep(seatstayDrivenGeometry(), 250));

        assertEquals("Shock stroke must be shorter than eye-to-eye length", error.getMessage());
    }

    /** The reference four-bar, with the shock driven by a seatstay point beyond the rocker pivot. */
    private HorstLinkSeatstayGeometry seatstayDrivenGeometry() {
        return new HorstLinkSeatstayGeometry(
                new Point2D(0, 0),
                new Point2D(-100, 0),
                new Point2D(0, -120),
                new Point2D(-100, -120),
                new Point2D(120, -180),
                new Point2D(-80, -160),
                new Point2D(-160, -90));
    }

    private void assertSamePoint(Point2D expected, Point2D actual) {
        assertEquals(expected.x(), actual.x(), DELTA);
        assertEquals(expected.y(), actual.y(), DELTA);
    }
}

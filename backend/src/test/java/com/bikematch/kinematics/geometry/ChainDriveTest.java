package com.bikematch.kinematics.geometry;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class ChainDriveTest {
    @Test
    void equalCogsGiveParallelUpperSpan() {
        var line = new ChainDrive(32, 32).at(new Point2D(400, 100), new Point2D(0, 0));
        assertEquals(Math.hypot(400, 100), line.lengthMm(), 1e-10);
        assertEquals(Math.atan2(100, 400), line.angleRadians(), 1e-10);
    }

    @Test
    void unequalCogsProduceATangentAtBothPitchCircles() {
        var drive = new ChainDrive(32, 50);
        var line = drive.at(new Point2D(450, 20), new Point2D(0, 0));
        double nx = Math.sin(line.angleRadians());
        double ny = -Math.cos(line.angleRadians());
        double delta = drive.chainringRadiusMm() - drive.sprocketRadiusMm();
        double tx = 450 + delta * nx;
        double ty = 20 + delta * ny;
        assertEquals(0, tx * nx + ty * ny, 1e-10);
        assertEquals(line.lengthMm(), Math.hypot(tx, ty), 1e-10);
    }

    @Test
    void overlappingOrNonFiniteCentresAreRejected() {
        var drive = new ChainDrive(32, 50);
        assertThrows(IllegalArgumentException.class, () -> drive.at(new Point2D(5, 0), new Point2D(0, 0)));
        assertThrows(IllegalArgumentException.class, () -> drive.at(new Point2D(400, Double.NaN), new Point2D(0, 0)));
        assertThrows(IllegalArgumentException.class, () -> new ChainDrive(0, 50));
    }
}

package com.bikematch.kinematics.curve;

import com.bikematch.kinematics.check.TravelCheck;
import com.bikematch.kinematics.descriptor.AxlePathDescriptors;
import com.bikematch.kinematics.descriptor.LeverageDescriptors;
import com.bikematch.kinematics.geometry.Point2D;
import com.bikematch.kinematics.solver.MonopivotSolver;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import java.util.List;
import static org.junit.jupiter.api.Assertions.assertThrows;

class CurveValidationTest {
    private final List<Point2D> path = List.of(new Point2D(10, 100), new Point2D(9, 50), new Point2D(10, 0));

    @ParameterizedTest
    @ValueSource(doubles = {0, -1, Double.NaN, Double.POSITIVE_INFINITY})
    void rejectsInvalidDivisors(double value) {
        assertThrows(IllegalArgumentException.class, () -> new MonopivotSolver().pivotAngle(value, 4, 5));
        assertThrows(IllegalArgumentException.class, () -> LeverageCurve.from(path, value));
        assertThrows(IllegalArgumentException.class, () -> TravelCheck.from(path, value));
    }

    @Test
    void rejectsEmptyAndStationaryPaths() {
        assertThrows(IllegalArgumentException.class, () -> LeverageCurve.from(List.of(), 50));
        assertThrows(IllegalArgumentException.class, () -> AxlePathDescriptors.from(List.of(new Point2D(0, 0))));
        var stationary = List.of(new Point2D(0, 0), new Point2D(1, 0));
        assertThrows(IllegalArgumentException.class, () -> TravelCheck.from(stationary, 150));
        assertThrows(IllegalArgumentException.class, () -> KickbackCurve.from(path, new Point2D(0, 0), 0));
    }

    @ParameterizedTest
    @ValueSource(doubles = {0, -1, 100, 150, Double.NaN})
    void rejectsSagOutsideTheWorkingInterval(double sag) {
        assertThrows(IllegalArgumentException.class, () -> LeverageDescriptors.from(LeverageCurve.from(path, 50), sag));
    }

    @Test
    void refusesInvalidLeverageBeforeClassifyingIt() {
        var invalid = new LeverageCurve(List.of(new LeverageSample(10, 2.5), new LeverageSample(100, 0)));
        assertThrows(IllegalArgumentException.class, () -> LeverageDescriptors.from(invalid, 30));
        assertThrows(IllegalArgumentException.class, () -> LeverageDescriptors.from(new LeverageCurve(List.of()), 30));
    }
}

package com.bikematch.kinematics.descriptor;

import com.bikematch.kinematics.curve.LeverageCurve;
import com.bikematch.kinematics.curve.LeverageSample;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class LeverageDescriptorsTest {

    /** Clean curve: LR from 3.0 down to 2.1, travel 10 -> 100 mm, in 0.1 steps. */
    private LeverageCurve descendingCurve() {
        List<LeverageSample> samples = List.of(
                new LeverageSample(10, 3.0),
                new LeverageSample(20, 2.9),
                new LeverageSample(30, 2.8),
                new LeverageSample(40, 2.7),
                new LeverageSample(50, 2.6),
                new LeverageSample(60, 2.5),
                new LeverageSample(70, 2.4),
                new LeverageSample(80, 2.3),
                new LeverageSample(90, 2.2),
                new LeverageSample(100, 2.1)
        );
        return new LeverageCurve(samples);
    }

    @Test
    void readsTheLeverageValues() {
        LeverageDescriptors d = LeverageDescriptors.from(descendingCurve(), 30);
        assertEquals(3.0, d.lrInitial(), 1e-9);
        assertEquals(2.1, d.lrFinal(), 1e-9);
        assertEquals(2.55, d.lrMean(), 1e-9);
    }

    @Test
    void picksTheLeverageAtTheSagHeight() {
        LeverageDescriptors d = LeverageDescriptors.from(descendingCurve(), 30);
        assertEquals(2.8, d.lrAtSag(), 1e-9);
    }

    @Test
    void computesTotalAndUsefulProgression() {
        LeverageDescriptors d = LeverageDescriptors.from(descendingCurve(), 30);
        assertEquals(42.857, d.totalProgressionPercent(), 0.001);
        assertEquals(33.333, d.usefulProgressionPercent(), 0.001);
    }

    @Test
    void computesTheSegmentSlopes() {
        LeverageDescriptors d = LeverageDescriptors.from(descendingCurve(), 30);
        assertEquals(-0.01, d.slopeInitialToSag(), 1e-9);
        assertEquals(-0.01, d.slopeSagToEnd(), 1e-9);
    }

    // --- shape as three phases (initial / middle / final third); band from useful progression ---

    /** A 4-point curve at travel 10 / 33 (1st third) / 66 (2nd third) / 100 mm. */
    private LeverageCurve curve(double lr10, double lr33, double lr66, double lr100) {
        return new LeverageCurve(List.of(
                new LeverageSample(10, lr10),
                new LeverageSample(33, lr33),
                new LeverageSample(66, lr66),
                new LeverageSample(100, lr100)));
    }

    @Test
    void classifiesAThreePhaseProgressiveCurve() {
        LeverageDescriptors d = LeverageDescriptors.from(curve(3.0, 2.7, 2.5, 2.3), 30);
        assertEquals(SegmentTrend.PROGRESSIVE, d.initialTrend());
        assertEquals(SegmentTrend.PROGRESSIVE, d.middleTrend());
        assertEquals(SegmentTrend.PROGRESSIVE, d.finalTrend());
    }

    @Test
    void classifiesAFlatCurveAsAllLinear() {
        LeverageDescriptors d = LeverageDescriptors.from(curve(2.55, 2.52, 2.49, 2.46), 30);
        assertEquals(SegmentTrend.LINEAR, d.initialTrend());
        assertEquals(SegmentTrend.LINEAR, d.middleTrend());
        assertEquals(SegmentTrend.LINEAR, d.finalTrend());
    }

    @Test
    void classifiesProgressiveLinearRegressive() {
        LeverageDescriptors d = LeverageDescriptors.from(curve(3.0, 2.6, 2.55, 2.75), 30);
        assertEquals(SegmentTrend.PROGRESSIVE, d.initialTrend());
        assertEquals(SegmentTrend.LINEAR, d.middleTrend());
        assertEquals(SegmentTrend.REGRESSIVE, d.finalTrend());
    }

    @Test
    void classifiesFullyRegressive() {
        LeverageDescriptors d = LeverageDescriptors.from(curve(2.4, 2.6, 2.8, 3.0), 30);
        assertEquals(SegmentTrend.REGRESSIVE, d.initialTrend());
        assertEquals(SegmentTrend.REGRESSIVE, d.middleTrend());
        assertEquals(SegmentTrend.REGRESSIVE, d.finalTrend());
    }

    @Test
    void detectsAFlatFinalThird() {
        // Progressive most of the way, but the last third flattens (bottom-out risk).
        LeverageDescriptors d = LeverageDescriptors.from(curve(3.0, 2.7, 2.4, 2.35), 30);
        assertEquals(SegmentTrend.PROGRESSIVE, d.initialTrend());
        assertEquals(SegmentTrend.PROGRESSIVE, d.middleTrend());
        assertEquals(SegmentTrend.LINEAR, d.finalTrend());
    }

    @Test
    void detectsARegressiveFinalThird() {
        LeverageDescriptors d = LeverageDescriptors.from(curve(3.0, 2.7, 2.5, 2.65), 30);
        assertEquals(SegmentTrend.PROGRESSIVE, d.initialTrend());
        assertEquals(SegmentTrend.PROGRESSIVE, d.middleTrend());
        assertEquals(SegmentTrend.REGRESSIVE, d.finalTrend());
    }

    @Test
    void classifiesTheProgressionBand() {
        // useful = (2.9 / 2.4 - 1) * 100 = 20.8% -> HIGH
        LeverageDescriptors d = LeverageDescriptors.from(curve(3.0, 2.9, 2.65, 2.4), 30);
        assertEquals(ProgressionBand.HIGH, d.progressionBand());
    }
}

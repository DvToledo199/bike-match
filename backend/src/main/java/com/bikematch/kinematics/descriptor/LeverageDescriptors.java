package com.bikematch.kinematics.descriptor;

import com.bikematch.kinematics.curve.LeverageCurve;
import com.bikematch.kinematics.curve.LeverageSample;
import com.bikematch.kinematics.curve.CurveChecks;

import java.util.List;

/**
 * Numeric descriptors of a leverage curve, plus its rule-based classification: the
 * useful-progression magnitude band and the curve shape read as three phases.
 *
 * <p>The whole 0→100% curve is kept. The sag (sagPercent of total travel, 30% by default)
 * drives the useful progression (sag→final, the working zone); total progression is
 * initial→final. Progression convention (base-conocimiento §3): (LR_ref / LR_final − 1) × 100.
 *
 * <p>The shape is read over the whole travel split into three equal thirds — initial, middle
 * and final — each classified as PROGRESSIVE (LR drops), LINEAR (LR ~flat) or REGRESSIVE
 * (LR rises). The final third is the bottom-out zone. The classic named curves are patterns
 * of these three phases (e.g. progressive-progressive-progressive = "continuous progressive";
 * regressive-…-progressive = "humped").
 */
public record LeverageDescriptors(
        double lrInitial,
        double lrAtSag,
        double lrFinal,
        double lrMean,
        double totalProgressionPercent,
        double usefulProgressionPercent,
        double slopeInitialToSag,
        double slopeSagToEnd,
        SegmentTrend initialTrend,
        SegmentTrend middleTrend,
        SegmentTrend finalTrend,
        ProgressionBand progressionBand) {

    /** An LR change within ±this over a third is treated as flat (base-conocimiento §3). */
    private static final double FLAT_BAND = 0.1;

    public static LeverageDescriptors from(LeverageCurve curve, double sagPercent) {
        if (curve == null || curve.samples() == null || curve.samples().size() < 2) {
            throw new IllegalArgumentException("Leverage descriptors need at least two samples");
        }
        if (!Double.isFinite(sagPercent) || sagPercent <= 0 || sagPercent >= 100) {
            throw new IllegalArgumentException("Sag must be strictly between 0 and 100 percent");
        }
        List<LeverageSample> samples = curve.samples();
        for (int i = 0; i < samples.size(); i++) {
            LeverageSample sample = samples.get(i);
            if (sample == null) throw new IllegalArgumentException("A leverage sample is missing");
            CurveChecks.positiveFinite(sample.ratio(), "Leverage ratio");
            CurveChecks.finite(sample.wheelTravelMm(), "Wheel travel");
            if (sample.wheelTravelMm() < 0) throw new IllegalArgumentException("Wheel travel cannot be negative");
            if (i > 0) CurveChecks.positiveFinite(sample.wheelTravelMm() - samples.get(i - 1).wheelTravelMm(), "Sample spacing");
        }
        LeverageSample first = samples.get(0);
        LeverageSample last = samples.get(samples.size() - 1);

        double lrInitial = first.ratio();
        double lrFinal = last.ratio();
        double totalTravelMm = last.wheelTravelMm();
        double sagTravelMm = totalTravelMm * sagPercent / 100.0;

        LeverageSample atSag = nearestToTravel(samples, sagTravelMm);
        double lrAtSag = atSag.ratio();

        double lrMean = meanRatio(samples);
        double totalProgression = (lrInitial / lrFinal - 1) * 100.0;
        double usefulProgression = (lrAtSag / lrFinal - 1) * 100.0;

        double slopeInitialToSag = (lrAtSag - lrInitial) / (atSag.wheelTravelMm() - first.wheelTravelMm());
        double slopeSagToEnd = (lrFinal - lrAtSag) / (last.wheelTravelMm() - atSag.wheelTravelMm());
        CurveChecks.finite(totalProgression, "Total progression");
        CurveChecks.finite(usefulProgression, "Useful progression");
        CurveChecks.finite(slopeInitialToSag, "Initial-to-sag slope (sample spacing)");
        CurveChecks.finite(slopeSagToEnd, "Sag-to-end slope (sample spacing)");

        // Shape read over three equal thirds of the travel.
        double lrAtThird1 = nearestToTravel(samples, totalTravelMm / 3.0).ratio();
        double lrAtThird2 = nearestToTravel(samples, totalTravelMm * 2.0 / 3.0).ratio();
        SegmentTrend initialTrend = trendOf(lrAtThird1 - lrInitial);
        SegmentTrend middleTrend = trendOf(lrAtThird2 - lrAtThird1);
        SegmentTrend finalTrend = trendOf(lrFinal - lrAtThird2);

        ProgressionBand progressionBand = classifyBand(usefulProgression);

        return new LeverageDescriptors(lrInitial, lrAtSag, lrFinal, lrMean,
                totalProgression, usefulProgression, slopeInitialToSag, slopeSagToEnd,
                initialTrend, middleTrend, finalTrend, progressionBand);
    }

    /** How a third behaves from its LR change: rise = softens, drop = hardens, else flat. */
    private static SegmentTrend trendOf(double deltaLr) {
        if (deltaLr > FLAT_BAND) {
            return SegmentTrend.REGRESSIVE;
        }
        if (deltaLr < -FLAT_BAND) {
            return SegmentTrend.PROGRESSIVE;
        }
        return SegmentTrend.LINEAR;
    }

    /** The magnitude band of the useful progression (base-conocimiento §3). */
    private static ProgressionBand classifyBand(double usefulProgressionPercent) {
        if (usefulProgressionPercent < 0) return ProgressionBand.REGRESSIVE;
        if (usefulProgressionPercent < 5) return ProgressionBand.LINEAR;
        if (usefulProgressionPercent < 12) return ProgressionBand.SLIGHTLY_PROGRESSIVE;
        if (usefulProgressionPercent < 20) return ProgressionBand.MEDIUM;
        if (usefulProgressionPercent <= 30) return ProgressionBand.HIGH;
        return ProgressionBand.VERY_HIGH;
    }

    /** The sample whose wheel travel is closest to the target (a split height). */
    private static LeverageSample nearestToTravel(List<LeverageSample> samples, double targetTravelMm) {
        LeverageSample nearest = samples.get(0);
        double bestDistance = Math.abs(nearest.wheelTravelMm() - targetTravelMm);
        for (LeverageSample sample : samples) {
            double distance = Math.abs(sample.wheelTravelMm() - targetTravelMm);
            if (distance < bestDistance) {
                bestDistance = distance;
                nearest = sample;
            }
        }
        return nearest;
    }

    private static double meanRatio(List<LeverageSample> samples) {
        double sum = 0;
        for (LeverageSample sample : samples) {
            sum += sample.ratio() / samples.size();
        }
        CurveChecks.finite(sum, "Average leverage ratio");
        return sum;
    }
}

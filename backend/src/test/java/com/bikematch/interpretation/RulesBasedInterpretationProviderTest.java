package com.bikematch.interpretation;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import org.junit.jupiter.api.Test;

class RulesBasedInterpretationProviderTest {

    private final RulesBasedInterpretationProvider provider = new RulesBasedInterpretationProvider();

    @Test
    void readsTheBikeWithoutInventingPersonalRecommendations() {
        Interpretation result = provider.generate(referenceContext(true, true));

        assertThat(result.source()).isEqualTo(Interpretation.Source.RULES);
        assertThat(result.providerVersion()).isEqualTo("rules-4");
        assertThat(result.summary())
                .contains("progressive leverage response")
                .contains("22%")
                .contains("These are geometric tendencies")
                .doesNotContain("pressure")
                .doesNotContain("clicks");
        assertThat(result.evidence()).hasSize(3);
    }

    /** Linear means predictable. "Reserve of its own" means nothing to a reader. */
    @Test
    void callsALinearBikePredictableAndLeavesItThere() {
        Interpretation result = provider.generate(contextWithEightFigures());

        assertThat(result.summary())
                .contains("linear leverage response")
                .contains("behaves predictably")
                .doesNotContain("reserve of its own");
    }

    /** The curve alone cannot choose a spring, so the fallback never names one. */
    @Test
    void neverRecommendsAShock() {
        Interpretation result = provider.generate(contextWithEightFigures());

        assertThat(result.summary())
                .doesNotContain("coil")
                .doesNotContain("spacer")
                .doesNotContain("shock absorber");
    }

    /** Pedalling is one sentence about where the effort goes, and the gear is not printed. */
    @Test
    void describesPedallingAsWhereTheEffortGoes() {
        Interpretation result = provider.generate(contextWithEightFigures());

        assertThat(result.summary())
                .contains("pedals in balance")
                .doesNotContain("32x52")
                .doesNotContain("fights the suspension")
                .doesNotContain("kickback");
    }

    /** No bike is made to sound bad, and braking is said the way the rider feels it. */
    @Test
    void readsBrakingAsTheRiderFeelsIt() {
        Interpretation result = provider.generate(contextWithEightFigures());

        assertThat(result.summary())
                .contains("the rear settles")
                .contains("more stable")
                .contains("as a consequence of not copying the ground as closely")
                .doesNotContain("squats");
    }

    @Test
    void saysALiftingRearFeelsLessSettledButCopiesTheGroundBetter() {
        Interpretation result = provider.generate(contextWithLiftingRear());

        assertThat(result.summary())
                .contains("the rear lifts")
                .contains("less settled")
                .contains("the slope feels steeper")
                .contains("copies the ground better");
    }

    /** Under 3 mm the axle path is not worth a sentence, so it does not get one. */
    @Test
    void staysSilentAboutAConventionalAxlePath() {
        Interpretation result = provider.generate(contextWithEightFigures());

        assertThat(result.summary()).doesNotContain("rearward");
    }

    @Test
    void citesOnlyFourFiguresWhenTheContextOffersMore() {
        Interpretation result = provider.generate(contextWithEightFigures());

        assertThat(result.evidence()).hasSize(4);
        assertThat(result.evidence()).extracting(InterpretationContext.Evidence::key)
                .containsExactly("totalProgressionPercent", "leverageRatioAtSag",
                        "maxRearwardMm", "maxKickbackDegrees");
    }

    @Test
    void warnsBeforeInterpretingWhenTravelCheckFails() {
        Interpretation result = provider.generate(referenceContext(false, true));

        assertThat(result.summary())
                .startsWith("The calculated travel does not match the declared travel")
                .contains("Review the photo marks and calibration")
                .doesNotContain("leverage response");
    }

    /** The context offers the whole menu of figures; a stored explanation may cite four. */
    private InterpretationContext contextWithEightFigures() {
        return context("LINEAR", "BALANCED", "SQUATS_UNDER_BRAKING");
    }

    private InterpretationContext contextWithLiftingRear() {
        return context("LINEAR", "BALANCED", "EXTENDS_UNDER_BRAKING");
    }

    private InterpretationContext context(String progression, String antiSquat, String antiRise) {
        return new InterpretationContext(
                4,
                1,
                "monopivot-reference-v2",
                "kinematics-rules-1",
                "en",
                new InterpretationContext.DataQuality(true, null),
                new InterpretationContext.Capabilities(true, true, true, true),
                new InterpretationContext.Conditions("ENDURO", 30.0, 32, 52),
                new InterpretationContext.LeverageShape(
                        progression, "LINEAR", "LINEAR", "LINEAR", 2.77, 2.75, 2.74, 2.71, 2.68),
                new InterpretationContext.Readings(
                        progression, antiSquat, antiRise, "MEDIUM", "TYPICAL",
                        "NOT_WORTH_MENTIONING"),
                List.of(
                        new InterpretationContext.Evidence("totalProgressionPercent", 3.3, "%"),
                        new InterpretationContext.Evidence("leverageRatioAtSag", 2.75, "ratio"),
                        new InterpretationContext.Evidence("maxRearwardMm", 1.4, "mm"),
                        new InterpretationContext.Evidence("maxKickbackDegrees", 29.4, "°"),
                        new InterpretationContext.Evidence("antiSquatAtSagPercent", 99.1, "%"),
                        new InterpretationContext.Evidence("antiRiseAtSagPercent", 80, "%"),
                        new InterpretationContext.Evidence("calculatedTravelMm", 149.9, "mm"),
                        new InterpretationContext.Evidence("usefulProgressionPercent", 2.5, "%")
                ),
                List.of("Marked-photo geometry; not a laboratory measurement."),
                List.of("leverage", "kickback", "riderFit"),
                List.of("pressure", "clicks", "productModels", "brands", "guarantees",
                        "springType", "volumeSpacers", "shockRecommendation", "axlePath")
        );
    }

    private InterpretationContext referenceContext(boolean travelCheckPassed, boolean referenceMetrics) {
        return new InterpretationContext(
                4,
                1,
                "monopivot-reference-v2",
                "kinematics-rules-1",
                "en",
                new InterpretationContext.DataQuality(
                        travelCheckPassed,
                        travelCheckPassed ? null : "Calculated travel differs from declared travel by 14%"
                ),
                new InterpretationContext.Capabilities(referenceMetrics, referenceMetrics, true, true),
                new InterpretationContext.Conditions("ENDURO", 30.0, 32, 52),
                new InterpretationContext.LeverageShape(
                        "HIGH", "PROGRESSIVE", "PROGRESSIVE", "LINEAR", 2.9, 2.8, 2.7, 2.5, 2.35),
                new InterpretationContext.Readings(
                        "HIGH",
                        referenceMetrics ? "FIRM" : null,
                        referenceMetrics ? "SQUATS_UNDER_BRAKING" : null,
                        "MEDIUM", "TYPICAL", "BEYOND_MODEL"),
                List.of(
                        new InterpretationContext.Evidence("totalProgressionPercent", 22, "%"),
                        new InterpretationContext.Evidence("maxRearwardMm", 12, "mm"),
                        new InterpretationContext.Evidence("maxKickbackDegrees", 32, "°")
                ),
                List.of("Analytical reference; not a personal setup recommendation."),
                List.of("leverage", "kickback", "antiSquat", "antiRise"),
                List.of("pressure", "clicks", "productModels", "brands", "guarantees",
                        "springType", "volumeSpacers", "shockRecommendation", "axlePath")
        );
    }
}

package com.bikematch.interpretation;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import org.junit.jupiter.api.Test;

class RulesBasedInterpretationProviderTest {

    private final RulesBasedInterpretationProvider provider = new RulesBasedInterpretationProvider();

    @Test
    void explainsReferenceMetricsWithoutInventingPersonalRecommendations() {
        Interpretation result = provider.generate(referenceContext(true, true));

        assertThat(result.source()).isEqualTo(Interpretation.Source.RULES);
        assertThat(result.providerVersion()).isEqualTo("rules-2");
        assertThat(result.summary())
                .contains("progressive leverage response")
                .contains("18%")
                .contains("12mm rearward. The anti-squat and anti-rise figures are reference-model estimates. These are geometric tendencies")
                .doesNotContain("pressure")
                .doesNotContain("clicks");
        assertThat(result.evidence()).hasSize(3);
    }

    @Test
    void endsTheMainSentenceWhenNoReferenceMetricsAreAvailable() {
        Interpretation result = provider.generate(referenceContext(true, false));

        assertThat(result.summary())
                .contains("12mm rearward. These are geometric tendencies")
                .doesNotContain("reference-model estimates");
    }

    @Test
    void warnsBeforeInterpretingWhenTravelCheckFails() {
        Interpretation result = provider.generate(referenceContext(false, true));

        assertThat(result.summary())
                .startsWith("The calculated travel does not match the declared travel")
                .contains("Review the photo marks and calibration")
                .doesNotContain("progressive leverage response");
    }

    private InterpretationContext referenceContext(boolean travelCheckPassed, boolean referenceMetrics) {
        return new InterpretationContext(
                1,
                1,
                "monopivot-reference-v2",
                "kinematics-rules-1",
                "en",
                new InterpretationContext.DataQuality(
                        travelCheckPassed,
                        travelCheckPassed ? null : "Calculated travel differs from declared travel by 14%"
                ),
                new InterpretationContext.Capabilities(referenceMetrics, referenceMetrics, true, true),
                List.of(
                        new InterpretationContext.Evidence("usefulProgressionPercent", 18, "%"),
                        new InterpretationContext.Evidence("maxRearwardMm", 12, "mm"),
                        new InterpretationContext.Evidence("kickbackDegrees", 32, "°")
                ),
                List.of("Analytical reference; not a personal setup recommendation."),
                List.of("leverage", "axlePath", "kickback", "antiSquat", "antiRise"),
                List.of("pressure", "clicks", "productModels", "riderSuitability")
        );
    }
}

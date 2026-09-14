package com.bikematch.interpretation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;

import com.bikematch.bike.KinematicsResult;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

class InterpretationContextFactoryTest {

    private final InterpretationContextFactory factory = new InterpretationContextFactory(new ObjectMapper());

    @Test
    void buildsSafeReferenceContextFromStoredJson() {
        KinematicsResult result = result(
                "monopivot-reference-v2",
                "{\"leverageDescriptors\":{\"usefulProgressionPercent\":18},"
                        + "\"axlePathDescriptors\":{\"maxRearwardMm\":12},"
                        + "\"travelCheck\":{\"calculatedTravelMm\":150,\"deviationPercent\":3.1,"
                        + "\"withinTolerance\":true}}",
                "{\"cogAwareKickback\":true,\"antiSquat\":true,\"antiRise\":true,\"referenceOnly\":true}",
                "{\"kickbackCurve\":[{\"kickbackDegrees\":4},{\"kickbackDegrees\":32}]}");

        InterpretationContext context = factory.create(result, "EN");

        assertThat(context.language()).isEqualTo("en");
        assertThat(context.capabilities().antiSquat()).isTrue();
        assertThat(context.allowedTopics()).contains("antiSquat", "antiRise");
        assertThat(context.forbiddenTopics()).contains("pressure", "riderSuitability");
        assertThat(context.evidence()).extracting(InterpretationContext.Evidence::key)
                .containsExactly("usefulProgressionPercent", "maxRearwardMm", "maxKickbackDegrees", "calculatedTravelMm");
    }

    @Test
    void marksBadTravelAndForbidsUnavailableReferenceTopics() {
        KinematicsResult result = result(
                "monopivot-v1",
                "{\"leverageDescriptors\":{\"usefulProgressionPercent\":2},"
                        + "\"axlePathDescriptors\":{\"maxRearwardMm\":1},"
                        + "\"travelCheck\":{\"deviationPercent\":14,\"withinTolerance\":false}}",
                "{\"cogAwareKickback\":false,\"antiSquat\":false,\"antiRise\":false,\"referenceOnly\":false}",
                "{\"kickbackCurve\":[{\"kickbackDegrees\":10}]}" );

        InterpretationContext context = factory.create(result, "en");

        assertThat(context.dataQuality().travelCheckPassed()).isFalse();
        assertThat(context.dataQuality().warning()).contains("14%");
        assertThat(context.forbiddenTopics()).contains("antiSquat", "antiRise", "sprocketInfluence");
        assertThat(context.allowedTopics()).doesNotContain("antiSquat", "antiRise");
    }

    private KinematicsResult result(String engineVersion, String descriptors,
                                    String capabilities, String curves) {
        KinematicsResult result = org.mockito.Mockito.mock(KinematicsResult.class);
        given(result.getResultVersion()).willReturn(1);
        given(result.getEngineVersion()).willReturn(engineVersion);
        given(result.getDescriptors()).willReturn(descriptors);
        given(result.getCapabilities()).willReturn(capabilities);
        given(result.getCurves()).willReturn(curves);
        return result;
    }
}

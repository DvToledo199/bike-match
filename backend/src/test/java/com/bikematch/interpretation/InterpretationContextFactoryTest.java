package com.bikematch.interpretation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;

import com.bikematch.bike.BikeCategory;
import com.bikematch.bike.KinematicsResult;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

class InterpretationContextFactoryTest {

    private final InterpretationContextFactory factory = new InterpretationContextFactory(new ObjectMapper());

    @Test
    void buildsSafeReferenceContextFromStoredJson() {
        KinematicsResult result = result(
                "monopivot-reference-v2",
                "{\"conditions\":{\"sagPercent\":30,\"chainringTeeth\":32,\"sprocketTeeth\":52},"
                        + "\"leverageDescriptors\":{\"usefulProgressionPercent\":18,"
                        + "\"totalProgressionPercent\":22,\"lrInitial\":2.9,\"lrAtSag\":2.8,\"lrFinal\":2.35,"
                        + "\"progressionBand\":\"MEDIUM\",\"initialTrend\":\"PROGRESSIVE\","
                        + "\"middleTrend\":\"PROGRESSIVE\",\"finalTrend\":\"LINEAR\"},"
                        + "\"axlePathDescriptors\":{\"maxRearwardMm\":12,\"atTravelPercent\":25},"
                        + "\"travelCheck\":{\"calculatedTravelMm\":150,\"deviationPercent\":3.1,"
                        + "\"withinTolerance\":true}}",
                "{\"cogAwareKickback\":true,\"antiSquat\":true,\"antiRise\":true,\"referenceOnly\":true}",
                "{\"kickbackCurve\":[{\"wheelTravelMm\":0,\"kickbackDegrees\":4},"
                        + "{\"wheelTravelMm\":45,\"kickbackDegrees\":32}],"
                        + "\"antiSquatCurve\":[{\"wheelTravelMm\":0,\"percent\":110},"
                        + "{\"wheelTravelMm\":45,\"percent\":104}],"
                        + "\"antiRiseCurve\":[{\"wheelTravelMm\":0,\"percent\":95},"
                        + "{\"wheelTravelMm\":45,\"percent\":80}]}");

        InterpretationContext context = factory.create(result, "EN", BikeCategory.ENDURO);

        assertThat(context.language()).isEqualTo("en");
        assertThat(context.capabilities().antiSquat()).isTrue();
        assertThat(context.conditions().bikeCategory()).isEqualTo("ENDURO");
        assertThat(context.conditions().sagPercent()).isEqualTo(30.0);
        assertThat(context.conditions().chainringTeeth()).isEqualTo(32);
        assertThat(context.conditions().sprocketTeeth()).isEqualTo(52);
        assertThat(context.leverageShape().progressionBand()).isEqualTo("MEDIUM");
        assertThat(context.leverageShape().finalTrend()).isEqualTo("LINEAR");
        assertThat(context.leverageShape().leverageRatioInitial()).isEqualTo(2.9);
        assertThat(context.leverageShape().leverageRatioFinal()).isEqualTo(2.35);
        assertThat(context.allowedTopics())
                .contains("antiSquat", "antiRise", "springType", "volumeSpacers", "riderFit");
        assertThat(context.forbiddenTopics())
                .contains("pressure", "clicks", "brands", "guarantees")
                .doesNotContain("riderFit", "springType");
        assertThat(context.evidence()).extracting(InterpretationContext.Evidence::key)
                .containsExactly("usefulProgressionPercent", "leverageRatioAtSag", "maxRearwardMm",
                        "maxKickbackDegrees", "antiSquatAtSagPercent", "antiRiseAtSagPercent",
                        "calculatedTravelMm", "totalProgressionPercent");
    }

    @Test
    void readsAntiSquatAndAntiRiseAtTheSagPoint() {
        KinematicsResult result = result(
                "monopivot-reference-v2",
                "{\"conditions\":{\"sagPercent\":30},"
                        + "\"leverageDescriptors\":{\"usefulProgressionPercent\":18,\"lrAtSag\":2.8},"
                        + "\"axlePathDescriptors\":{\"maxRearwardMm\":2},"
                        + "\"travelCheck\":{\"calculatedTravelMm\":150,\"withinTolerance\":true}}",
                "{\"cogAwareKickback\":true,\"antiSquat\":true,\"antiRise\":false,\"referenceOnly\":true}",
                "{\"antiSquatCurve\":[{\"wheelTravelMm\":0,\"percent\":110},"
                        + "{\"wheelTravelMm\":45,\"percent\":104},"
                        + "{\"wheelTravelMm\":120,\"percent\":70}]}");

        InterpretationContext context = factory.create(result, "en", BikeCategory.DOWNHILL);

        // Sag is 30% of 150 mm, so the reading is the sample at 45 mm, not the first or the last.
        assertThat(context.evidence())
                .filteredOn(item -> item.key().equals("antiSquatAtSagPercent"))
                .singleElement()
                .extracting(InterpretationContext.Evidence::value)
                .isEqualTo(104.0);
        assertThat(context.evidence()).extracting(InterpretationContext.Evidence::key)
                .doesNotContain("antiRiseAtSagPercent");
    }

    @Test
    void marksBadTravelAndForbidsUnavailableReferenceTopics() {
        KinematicsResult result = result(
                "monopivot-v1",
                "{\"leverageDescriptors\":{\"usefulProgressionPercent\":2},"
                        + "\"axlePathDescriptors\":{\"maxRearwardMm\":1},"
                        + "\"travelCheck\":{\"deviationPercent\":14,\"withinTolerance\":false}}",
                "{\"cogAwareKickback\":false,\"antiSquat\":false,\"antiRise\":false,\"referenceOnly\":false}",
                "{\"kickbackCurve\":[{\"wheelTravelMm\":0,\"kickbackDegrees\":10}]}" );

        InterpretationContext context = factory.create(result, "en", BikeCategory.ENDURO);

        assertThat(context.dataQuality().travelCheckPassed()).isFalse();
        assertThat(context.dataQuality().warning()).contains("14%");
        assertThat(context.forbiddenTopics()).contains("antiSquat", "antiRise", "sprocketInfluence");
        assertThat(context.allowedTopics()).doesNotContain("antiSquat", "antiRise");
        assertThat(context.conditions().sagPercent()).isNull();
        assertThat(context.leverageShape().progressionBand()).isNull();
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

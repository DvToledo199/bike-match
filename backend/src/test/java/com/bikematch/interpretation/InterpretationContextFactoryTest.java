package com.bikematch.interpretation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
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
                        + "\"lrMean\":2.7,\"progressionBand\":\"MEDIUM\",\"initialTrend\":\"PROGRESSIVE\","
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

        assertThat(context.interpretationContextVersion()).isEqualTo(4);
        assertThat(context.language()).isEqualTo("en");
        assertThat(context.capabilities().antiSquat()).isTrue();
        assertThat(context.conditions().bikeCategory()).isEqualTo("ENDURO");
        assertThat(context.conditions().sagPercent()).isEqualTo(30.0);
        assertThat(context.conditions().chainringTeeth()).isEqualTo(32);
        assertThat(context.conditions().sprocketTeeth()).isEqualTo(52);
        assertThat(context.leverageShape().leverageRatioInitial()).isEqualTo(2.9);
        assertThat(context.leverageShape().leverageRatioFinal()).isEqualTo(2.35);
        // The figure shown leads the menu, and the one the engine favours closes it.
        assertThat(context.evidence()).extracting(InterpretationContext.Evidence::key)
                .containsExactly("totalProgressionPercent", "leverageRatioAtSag", "maxRearwardMm",
                        "maxKickbackDegrees", "antiSquatAtSagPercent", "antiRiseAtSagPercent",
                        "calculatedTravelMm", "usefulProgressionPercent");
    }

    /**
     * The engine bands the useful progression; the reader is shown the total. Classifying the
     * total here keeps the band and the printed figure from saying different things: 22% over
     * the whole travel is HIGH, while the engine's own band for this bike is MEDIUM.
     */
    @Test
    void bandsTheProgressionItActuallyShows() {
        KinematicsResult result = result(
                "monopivot-reference-v2",
                "{\"conditions\":{\"sagPercent\":30},"
                        + "\"leverageDescriptors\":{\"usefulProgressionPercent\":18,"
                        + "\"totalProgressionPercent\":22,\"lrAtSag\":2.8,\"progressionBand\":\"MEDIUM\"},"
                        + "\"axlePathDescriptors\":{\"maxRearwardMm\":2},"
                        + "\"travelCheck\":{\"calculatedTravelMm\":150,\"withinTolerance\":true}}",
                "{\"cogAwareKickback\":false,\"antiSquat\":false,\"antiRise\":false,\"referenceOnly\":false}",
                "{}");

        InterpretationContext context = factory.create(result, "en", BikeCategory.ENDURO);

        assertThat(context.readings().progression()).isEqualTo("HIGH");
        assertThat(context.leverageShape().progressionBand()).isEqualTo("HIGH");
    }

    /** The bands are the vocabulary the provider writes with, so the factory has to name them. */
    @Test
    void namesTheBandEveryFigureFallsInto() {
        KinematicsResult result = result(
                "monopivot-reference-v2",
                "{\"conditions\":{\"sagPercent\":30,\"chainringTeeth\":32,\"sprocketTeeth\":52},"
                        + "\"leverageDescriptors\":{\"usefulProgressionPercent\":2.5,"
                        + "\"totalProgressionPercent\":3.3,\"lrAtSag\":2.75,"
                        + "\"lrMean\":2.73,\"progressionBand\":\"LINEAR\"},"
                        + "\"axlePathDescriptors\":{\"maxRearwardMm\":1.4},"
                        + "\"travelCheck\":{\"calculatedTravelMm\":150,\"withinTolerance\":true}}",
                "{\"cogAwareKickback\":true,\"antiSquat\":true,\"antiRise\":true,\"referenceOnly\":true}",
                "{\"kickbackCurve\":[{\"wheelTravelMm\":0,\"kickbackDegrees\":0},"
                        + "{\"wheelTravelMm\":45,\"kickbackDegrees\":29.4}],"
                        + "\"antiSquatCurve\":[{\"wheelTravelMm\":45,\"percent\":99.1}],"
                        + "\"antiRiseCurve\":[{\"wheelTravelMm\":45,\"percent\":80}]}");

        InterpretationContext context = factory.create(result, "en", BikeCategory.ENDURO);

        assertThat(context.readings().progression()).isEqualTo("LINEAR");
        assertThat(context.readings().antiSquat()).isEqualTo("BALANCED");
        assertThat(context.readings().antiRise()).isEqualTo("SQUATS_UNDER_BRAKING");
        assertThat(context.readings().kickback()).isEqualTo("MEDIUM");
        assertThat(context.readings().meanLeverage()).isEqualTo("TYPICAL");
        // Under 3 mm the axle path is not worth a sentence, so the topic is closed off.
        assertThat(context.readings().axlePath()).isEqualTo("NOT_WORTH_MENTIONING");
        assertThat(context.forbiddenTopics()).contains("axlePath");
        assertThat(context.allowedTopics()).doesNotContain("axlePath");
    }

    /** The initial feel does not end at sag: the sections are 0-40, 40-70 and 70-100. */
    @Test
    void readsTheCurveInThreeSectionsOfTravel() {
        KinematicsResult result = result(
                "monopivot-reference-v2",
                "{\"conditions\":{\"sagPercent\":30},"
                        + "\"leverageDescriptors\":{\"usefulProgressionPercent\":18,"
                        + "\"totalProgressionPercent\":18,\"lrAtSag\":2.8},"
                        + "\"axlePathDescriptors\":{\"maxRearwardMm\":1},"
                        + "\"travelCheck\":{\"calculatedTravelMm\":100,\"withinTolerance\":true}}",
                "{\"cogAwareKickback\":false,\"antiSquat\":false,\"antiRise\":false,\"referenceOnly\":false}",
                "{\"leverageCurve\":[{\"wheelTravelMm\":0,\"ratio\":3.0},"
                        + "{\"wheelTravelMm\":40,\"ratio\":2.5},"
                        + "{\"wheelTravelMm\":70,\"ratio\":2.5},"
                        + "{\"wheelTravelMm\":100,\"ratio\":2.9}]}");

        InterpretationContext context = factory.create(result, "en", BikeCategory.ENDURO);

        assertThat(context.leverageShape().initialFeelTrend()).isEqualTo("PROGRESSIVE");
        assertThat(context.leverageShape().midSupportTrend()).isEqualTo("LINEAR");
        // The leverage rises again over the last section: the worst place to lose support.
        assertThat(context.leverageShape().bottomOutTrend()).isEqualTo("REGRESSIVE");
        assertThat(context.leverageShape().leverageRatioAt40()).isEqualTo(2.5);
        assertThat(context.leverageShape().leverageRatioAt70()).isEqualTo(2.5);
    }

    /** Above 10 mm the direct-chain model does not represent the bike, so we say so. */
    @Test
    void declaresTheLimitWhenTheAxlePathIsBeyondTheChainModel() {
        KinematicsResult result = result(
                "monopivot-reference-v2",
                "{\"conditions\":{\"sagPercent\":30},"
                        + "\"leverageDescriptors\":{\"totalProgressionPercent\":18,\"lrAtSag\":2.8},"
                        + "\"axlePathDescriptors\":{\"maxRearwardMm\":18},"
                        + "\"travelCheck\":{\"calculatedTravelMm\":150,\"withinTolerance\":true}}",
                "{\"cogAwareKickback\":true,\"antiSquat\":false,\"antiRise\":false,\"referenceOnly\":true}",
                "{\"kickbackCurve\":[{\"wheelTravelMm\":45,\"kickbackDegrees\":40}]}");

        InterpretationContext context = factory.create(result, "en", BikeCategory.DOWNHILL);

        assertThat(context.readings().axlePath()).isEqualTo("BEYOND_MODEL");
        assertThat(context.forbiddenTopics()).contains("axlePath");
        assertThat(context.limits()).anyMatch(limit -> limit.contains("idler"));
    }

    /** The curve alone cannot choose a spring, so the provider is not allowed to try. */
    @Test
    void forbidsShockAdvice() {
        KinematicsResult result = result(
                "monopivot-reference-v2",
                "{\"conditions\":{\"sagPercent\":30},"
                        + "\"leverageDescriptors\":{\"totalProgressionPercent\":18,\"lrAtSag\":2.8},"
                        + "\"axlePathDescriptors\":{\"maxRearwardMm\":2},"
                        + "\"travelCheck\":{\"calculatedTravelMm\":150,\"withinTolerance\":true}}",
                "{\"cogAwareKickback\":true,\"antiSquat\":true,\"antiRise\":true,\"referenceOnly\":true}",
                "{\"antiSquatCurve\":[{\"wheelTravelMm\":45,\"percent\":104}],"
                        + "\"antiRiseCurve\":[{\"wheelTravelMm\":45,\"percent\":80}]}");

        InterpretationContext context = factory.create(result, "en", BikeCategory.ENDURO);

        assertThat(context.forbiddenTopics())
                .contains("springType", "volumeSpacers", "shockRecommendation");
        assertThat(context.allowedTopics())
                .doesNotContain("springType", "volumeSpacers")
                .contains("riderFit");
    }

    @Test
    void acceptsSpanishAndRejectsLanguagesTheProvidersCannotWrite() {
        KinematicsResult result = result(
                "monopivot-reference-v2",
                "{\"conditions\":{\"sagPercent\":30},"
                        + "\"leverageDescriptors\":{\"totalProgressionPercent\":18,\"lrAtSag\":2.8},"
                        + "\"axlePathDescriptors\":{\"maxRearwardMm\":2},"
                        + "\"travelCheck\":{\"calculatedTravelMm\":150,\"withinTolerance\":true}}",
                "{\"cogAwareKickback\":true,\"antiSquat\":false,\"antiRise\":false,\"referenceOnly\":true}",
                "{}");

        assertThat(factory.create(result, " ES ", BikeCategory.ENDURO).language()).isEqualTo("es");
        assertThatThrownBy(() -> factory.create(result, "fr", BikeCategory.ENDURO))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Spanish (es)");
    }

    @Test
    void readsAntiSquatAndAntiRiseAtTheSagPoint() {
        KinematicsResult result = result(
                "monopivot-reference-v2",
                "{\"conditions\":{\"sagPercent\":30},"
                        + "\"leverageDescriptors\":{\"totalProgressionPercent\":18,\"lrAtSag\":2.8},"
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
        assertThat(context.readings().antiRise()).isNull();
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
        assertThat(context.readings().kickback()).isNull();
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

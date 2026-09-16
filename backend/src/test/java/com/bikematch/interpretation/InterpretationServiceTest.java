package com.bikematch.interpretation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.bikematch.bike.Bike;
import com.bikematch.bike.BikeRepository;
import com.bikematch.bike.KinematicsResult;
import com.bikematch.bike.KinematicsResultRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;

@ExtendWith({MockitoExtension.class, OutputCaptureExtension.class})
class InterpretationServiceTest {

    private static final String PROMPT_VERSION = "interpretation-prompt-1";
    private static final String AI_EVIDENCE =
            "[{\"key\":\"usefulProgressionPercent\",\"value\":18,\"unit\":\"%\"}]";
    private static final String RULES_EVIDENCE =
            "[{\"key\":\"maxRearwardMm\",\"value\":12,\"unit\":\"mm\"}]";

    @Mock private BikeRepository bikeRepository;
    @Mock private KinematicsResultRepository resultRepository;
    @Mock private KinematicsInterpretationRepository interpretationRepository;
    @Mock private InterpretationContextFactory contextFactory;
    @Mock private InterpretationProviderSelector providerSelector;
    @Mock private InterpretationGenerationLimiter generationLimiter;
    @Mock private InterpretationProvider provider;
    @Mock private RulesBasedInterpretationProvider fallback;
    @Mock private Bike bike;
    @Mock private KinematicsResult result;

    private InterpretationService service;
    private InterpretationContext context;

    @BeforeEach
    void setUp() {
        service = new InterpretationService(
                bikeRepository, resultRepository, interpretationRepository,
                contextFactory, providerSelector, generationLimiter, new ObjectMapper());
        context = new InterpretationContext(
                1, 1, "monopivot-reference-v2", "kinematics-rules-1", "en",
                new InterpretationContext.DataQuality(true, null),
                new InterpretationContext.Capabilities(true, true, true, true),
                new InterpretationContext.Conditions("ENDURO", 30.0, 32, 52),
                new InterpretationContext.LeverageShape(
                        "MEDIUM", "PROGRESSIVE", "PROGRESSIVE", "LINEAR", 2.9, 2.8, 2.35),
                java.util.List.of(
                        new InterpretationContext.Evidence("usefulProgressionPercent", 18, "%"),
                        new InterpretationContext.Evidence("maxRearwardMm", 12, "mm")),
                java.util.List.of("reference"), java.util.List.of("leverage"), java.util.List.of("pressure"));
        given(bikeRepository.findById(7L)).willReturn(Optional.of(bike));
        given(bike.isOwnedBy(42L)).willReturn(true);
        given(resultRepository.findByBikeId(7L)).willReturn(Optional.of(result));
        given(result.getId()).willReturn(9L);
        given(contextFactory.create(result, "en", null)).willReturn(context);
        given(providerSelector.current()).willReturn(provider);
        given(provider.providerVersion()).willReturn("gemini-test");
        given(provider.promptVersion()).willReturn(PROMPT_VERSION);
    }

    @Test
    void generatesAndStoresExplanationUsingSelectedProvider() {
        allowGeneration();
        storedFor("gemini-test", Optional.empty());
        Interpretation generated = aiInterpretation();
        given(provider.generate(context)).willReturn(generated);
        given(interpretationRepository.saveAndFlush(any(KinematicsInterpretation.class)))
                .willReturn(stored(generated, AI_EVIDENCE));

        InterpretationService.InterpretationView view = service.generate(7L, 42L, "en");

        assertThat(view.source()).isEqualTo("AI");
        assertThat(view.summary()).isEqualTo("A concise explanation.");
        verify(provider).generate(context);
        verify(fallback, never()).generate(any());
        // A bike whose only explanation comes from the fallback must still reach the selected provider.
        verify(interpretationRepository, never())
                .findByKinematicsResultIdAndResultVersionAndInterpretationContextVersionAndRulesVersionAndLanguageAndProviderVersionAndPromptVersion(
                        9L, 1, 1, "kinematics-rules-1", "en", "rules-1", PROMPT_VERSION);
    }

    @Test
    void reusesTheStoredExplanationOfTheSelectedProvider() {
        storedFor("gemini-test", Optional.of(stored(aiInterpretation(), AI_EVIDENCE)));

        InterpretationService.InterpretationView view = service.generate(7L, 42L, "en");

        assertThat(view.source()).isEqualTo("AI");
        verify(provider, never()).generate(any());
        verify(generationLimiter, never()).tryAcquire(anyLong(), anyLong());
    }

    @Test
    void fallsBackToRulesWhenTheSelectedProviderFails() {
        allowGeneration();
        storedFor("gemini-test", Optional.empty());
        given(provider.generate(context)).willThrow(new InterpretationProviderException("offline"));
        given(providerSelector.fallback()).willReturn(fallback);
        given(fallback.providerVersion()).willReturn("rules-1");
        given(fallback.promptVersion()).willReturn(PROMPT_VERSION);
        storedFor("rules-1", Optional.empty());
        Interpretation generated = rulesInterpretation();
        given(fallback.generate(context)).willReturn(generated);
        given(interpretationRepository.saveAndFlush(any(KinematicsInterpretation.class)))
                .willReturn(stored(generated, RULES_EVIDENCE));

        InterpretationService.InterpretationView view = service.generate(7L, 42L, "en");

        assertThat(view.source()).isEqualTo("RULES");
        verify(fallback).generate(context);
    }

    @Test
    void reusesTheStoredRulesExplanationAndLogsWhyTheProviderFailed(CapturedOutput output) {
        allowGeneration();
        storedFor("gemini-test", Optional.empty());
        given(provider.generate(context))
                .willThrow(new InterpretationProviderException("quota exhausted"));
        given(providerSelector.fallback()).willReturn(fallback);
        given(fallback.providerVersion()).willReturn("rules-1");
        given(fallback.promptVersion()).willReturn(PROMPT_VERSION);
        storedFor("rules-1", Optional.of(stored(rulesInterpretation(), RULES_EVIDENCE)));

        InterpretationService.InterpretationView view = service.generate(7L, 42L, "en");

        assertThat(view.source()).isEqualTo("RULES");
        // Storing a second rules explanation for the same context would break the unique key.
        verify(fallback, never()).generate(any());
        verify(interpretationRepository, never()).saveAndFlush(any(KinematicsInterpretation.class));
        assertThat(output.getAll()).contains("quota exhausted");
    }

    private void allowGeneration() {
        given(generationLimiter.tryAcquire(42L, 7L)).willReturn(true);
    }

    private void storedFor(String providerVersion, Optional<KinematicsInterpretation> interpretation) {
        given(interpretationRepository
                .findByKinematicsResultIdAndResultVersionAndInterpretationContextVersionAndRulesVersionAndLanguageAndProviderVersionAndPromptVersion(
                        9L, 1, 1, "kinematics-rules-1", "en", providerVersion, PROMPT_VERSION))
                .willReturn(interpretation);
    }

    private Interpretation aiInterpretation() {
        return new Interpretation(
                "A concise explanation.", Interpretation.Source.AI, "gemini-test", context.evidence());
    }

    private Interpretation rulesInterpretation() {
        return new Interpretation(
                "Rules explanation.", Interpretation.Source.RULES, "rules-1", context.evidence());
    }

    private KinematicsInterpretation stored(Interpretation interpretation, String evidence) {
        return new KinematicsInterpretation(result, context, interpretation, PROMPT_VERSION, evidence);
    }
}

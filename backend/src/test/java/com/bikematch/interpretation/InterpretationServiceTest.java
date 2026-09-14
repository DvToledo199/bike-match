package com.bikematch.interpretation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
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
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(MockitoExtension.class)
class InterpretationServiceTest {

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
                java.util.List.of(
                        new InterpretationContext.Evidence("usefulProgressionPercent", 18, "%"),
                        new InterpretationContext.Evidence("maxRearwardMm", 12, "mm")),
                java.util.List.of("reference"), java.util.List.of("leverage"), java.util.List.of("pressure"));
        given(bikeRepository.findById(7L)).willReturn(Optional.of(bike));
        given(bike.isOwnedBy(42L)).willReturn(true);
        given(resultRepository.findByBikeId(7L)).willReturn(Optional.of(result));
        given(result.getId()).willReturn(9L);
        given(contextFactory.create(result, "en")).willReturn(context);
        given(providerSelector.current()).willReturn(provider);
        given(providerSelector.fallback()).willReturn(fallback);
        given(generationLimiter.tryAcquire(42L, 7L)).willReturn(true);
        given(provider.providerVersion()).willReturn("gemini-test");
        given(provider.promptVersion()).willReturn("interpretation-prompt-1");
        given(fallback.providerVersion()).willReturn("rules-1");
        given(fallback.promptVersion()).willReturn("interpretation-prompt-1");
    }

    @Test
    void generatesAndStoresExplanationUsingSelectedProvider() {
        Interpretation generated = new Interpretation(
                "A concise explanation.", Interpretation.Source.AI, "gemini-test", context.evidence());
        given(interpretationRepository
                .findByKinematicsResultIdAndResultVersionAndInterpretationContextVersionAndRulesVersionAndLanguageAndProviderVersionAndPromptVersion(
                        9L, 1, 1, "kinematics-rules-1", "en", "gemini-test", "interpretation-prompt-1"))
                .willReturn(Optional.empty());
        given(provider.generate(context)).willReturn(generated);
        KinematicsInterpretation saved = new KinematicsInterpretation(
                result, context, generated, "interpretation-prompt-1",
                "[{\"key\":\"usefulProgressionPercent\",\"value\":18,\"unit\":\"%\"}]" );
        given(interpretationRepository.saveAndFlush(any(KinematicsInterpretation.class))).willReturn(saved);

        InterpretationService.InterpretationView view = service.generate(7L, 42L, "en");

        assertThat(view.source()).isEqualTo("AI");
        assertThat(view.summary()).isEqualTo("A concise explanation.");
        verify(provider).generate(context);
        verify(fallback, never()).generate(any());
    }

    @Test
    void fallsBackToRulesWhenExternalProviderFails() {
        Interpretation generated = new Interpretation(
                "Rules explanation.", Interpretation.Source.RULES, "rules-1", context.evidence());
        given(interpretationRepository
                .findByKinematicsResultIdAndResultVersionAndInterpretationContextVersionAndRulesVersionAndLanguageAndProviderVersionAndPromptVersion(
                        9L, 1, 1, "kinematics-rules-1", "en", "gemini-test", "interpretation-prompt-1"))
                .willReturn(Optional.empty());
        given(interpretationRepository
                .findByKinematicsResultIdAndResultVersionAndInterpretationContextVersionAndRulesVersionAndLanguageAndProviderVersionAndPromptVersion(
                        9L, 1, 1, "kinematics-rules-1", "en", "rules-1", "interpretation-prompt-1"))
                .willReturn(Optional.empty());
        given(provider.generate(context)).willThrow(new InterpretationProviderException("offline"));
        given(fallback.generate(context)).willReturn(generated);
        KinematicsInterpretation saved = new KinematicsInterpretation(
                result, context, generated, "interpretation-prompt-1",
                "[{\"key\":\"maxRearwardMm\",\"value\":12,\"unit\":\"mm\"}]" );
        given(interpretationRepository.saveAndFlush(any(KinematicsInterpretation.class))).willReturn(saved);

        InterpretationService.InterpretationView view = service.generate(7L, 42L, "en");

        assertThat(view.source()).isEqualTo("RULES");
        verify(fallback).generate(context);
    }
}

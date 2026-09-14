package com.bikematch.interpretation;

import com.bikematch.bike.Bike;
import com.bikematch.bike.BikeNotFoundException;
import com.bikematch.bike.BikeRepository;
import com.bikematch.bike.KinematicsResult;
import com.bikematch.bike.KinematicsResultRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Instant;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class InterpretationService {

    private final BikeRepository bikeRepository;
    private final KinematicsResultRepository resultRepository;
    private final KinematicsInterpretationRepository interpretationRepository;
    private final InterpretationContextFactory contextFactory;
    private final InterpretationProviderSelector providerSelector;
    private final InterpretationGenerationLimiter generationLimiter;
    private final ObjectMapper objectMapper;

    public InterpretationService(
            BikeRepository bikeRepository,
            KinematicsResultRepository resultRepository,
            KinematicsInterpretationRepository interpretationRepository,
            InterpretationContextFactory contextFactory,
            InterpretationProviderSelector providerSelector,
            InterpretationGenerationLimiter generationLimiter,
            ObjectMapper objectMapper
    ) {
        this.bikeRepository = bikeRepository;
        this.resultRepository = resultRepository;
        this.interpretationRepository = interpretationRepository;
        this.contextFactory = contextFactory;
        this.providerSelector = providerSelector;
        this.generationLimiter = generationLimiter;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public InterpretationView generate(long bikeId, long ownerId, String language) {
        Bike bike = bikeRepository.findById(bikeId)
                .filter(foundBike -> foundBike.isOwnedBy(ownerId))
                .orElseThrow(BikeNotFoundException::new);
        KinematicsResult result = resultFor(bikeId);
        InterpretationContext context = contextFactory.create(result, language);
        InterpretationProvider provider = providerSelector.current();

        var cached = findCached(result, context, provider);
        if (cached.isPresent()) {
            return toView(cached.get());
        }
        if (!generationLimiter.tryAcquire(ownerId, bikeId)) {
            throw new InterpretationRateLimitException();
        }

        Interpretation interpretation;
        InterpretationProvider usedProvider = provider;
        try {
            interpretation = provider.generate(context);
        } catch (InterpretationProviderException exception) {
            usedProvider = providerSelector.fallback();
            interpretation = usedProvider.generate(context);
        }

        KinematicsInterpretation saved = interpretationRepository.saveAndFlush(
                new KinematicsInterpretation(
                        result, context, interpretation, usedProvider.promptVersion(),
                        toJson(interpretation.evidence())));
        return toView(saved);
    }

    @Transactional(readOnly = true)
    public InterpretationView get(long bikeId, Long viewerId, String language) {
        Bike bike = bikeRepository.findById(bikeId)
                .filter(foundBike -> foundBike.canBeViewedBy(viewerId))
                .orElseThrow(BikeNotFoundException::new);
        KinematicsResult result = resultFor(bike.getId());
        InterpretationContext context = contextFactory.create(result, language);

        return findCached(result, context, providerSelector.current())
                .map(this::toView)
                .orElseThrow(InterpretationNotAvailableException::new);
    }

    private java.util.Optional<KinematicsInterpretation> findCached(
            KinematicsResult result,
            InterpretationContext context,
            InterpretationProvider preferredProvider
    ) {
        var cached = findCachedForProvider(
                result, context, preferredProvider.providerVersion(), preferredProvider.promptVersion());
        InterpretationProvider fallback = providerSelector.fallback();
        if (cached.isPresent() || sameProvider(preferredProvider, fallback)) {
            return cached;
        }
        return findCachedForProvider(result, context, fallback.providerVersion(), fallback.promptVersion());
    }

    private boolean sameProvider(InterpretationProvider first, InterpretationProvider second) {
        return first.providerVersion().equals(second.providerVersion())
                && first.promptVersion().equals(second.promptVersion());
    }

    private java.util.Optional<KinematicsInterpretation> findCachedForProvider(
            KinematicsResult result,
            InterpretationContext context,
            String providerVersion,
            String promptVersion
    ) {
        return interpretationRepository
                .findByKinematicsResultIdAndResultVersionAndInterpretationContextVersionAndRulesVersionAndLanguageAndProviderVersionAndPromptVersion(
                        result.getId(), context.resultVersion(), context.interpretationContextVersion(),
                        context.rulesVersion(), context.language(), providerVersion, promptVersion);
    }

    private KinematicsResult resultFor(long bikeId) {
        return resultRepository.findByBikeId(bikeId)
                .orElseThrow(InterpretationNotAvailableException::new);
    }

    private InterpretationView toView(KinematicsInterpretation interpretation) {
        try {
            JsonNode evidence = objectMapper.readTree(interpretation.getEvidence());
            return new InterpretationView(
                    interpretation.getResultVersion(),
                    interpretation.getInterpretationContextVersion(),
                    interpretation.getRulesVersion(),
                    interpretation.getLanguage(),
                    interpretation.getSource(),
                    interpretation.getProviderVersion(),
                    interpretation.getSummary(),
                    evidence,
                    interpretation.getGeneratedAt());
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Stored interpretation evidence is not valid JSON", exception);
        }
    }

    private String toJson(List<InterpretationContext.Evidence> evidence) {
        try {
            return objectMapper.writeValueAsString(evidence);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Interpretation evidence could not be serialized", exception);
        }
    }

    public record InterpretationView(
            int resultVersion,
            int interpretationContextVersion,
            String rulesVersion,
            String language,
            String source,
            String providerVersion,
            String summary,
            JsonNode evidence,
            Instant generatedAt
    ) {
    }
}

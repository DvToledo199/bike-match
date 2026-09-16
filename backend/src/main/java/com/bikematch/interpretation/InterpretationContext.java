package com.bikematch.interpretation;

import java.util.List;
import java.util.Objects;

/**
 * Versioned, privacy-safe input prepared for an interpretation provider.
 * It contains selected evidence, not the photo, marked points or account data.
 *
 * <p>The evidence list is the menu of figures a provider may cite; the shape and the
 * conditions are what turn those figures into a reading of the bike. Both may be absent
 * for results calculated by an engine that did not report them.
 */
public record InterpretationContext(
        int interpretationContextVersion,
        int resultVersion,
        String engineVersion,
        String rulesVersion,
        String language,
        DataQuality dataQuality,
        Capabilities capabilities,
        Conditions conditions,
        LeverageShape leverageShape,
        List<Evidence> evidence,
        List<String> limits,
        List<String> allowedTopics,
        List<String> forbiddenTopics
) {

    public static final int MIN_EVIDENCE = 2;
    public static final int MAX_EVIDENCE = 8;

    public InterpretationContext {
        if (interpretationContextVersion < 1 || resultVersion < 1) {
            throw new IllegalArgumentException("Interpretation versions must be positive");
        }
        engineVersion = requireText(engineVersion, "Engine version");
        rulesVersion = requireText(rulesVersion, "Rules version");
        language = requireText(language, "Language");
        dataQuality = Objects.requireNonNull(dataQuality, "Data quality is required");
        capabilities = Objects.requireNonNull(capabilities, "Capabilities are required");
        evidence = List.copyOf(Objects.requireNonNull(evidence, "Evidence is required"));
        limits = List.copyOf(Objects.requireNonNull(limits, "Limits are required"));
        allowedTopics = List.copyOf(Objects.requireNonNull(allowedTopics, "Allowed topics are required"));
        forbiddenTopics = List.copyOf(Objects.requireNonNull(forbiddenTopics, "Forbidden topics are required"));
        if (evidence.size() < MIN_EVIDENCE || evidence.size() > MAX_EVIDENCE) {
            throw new IllegalArgumentException(
                    "Interpretation evidence must contain between 2 and 8 items");
        }
    }

    public record DataQuality(boolean travelCheckPassed, String warning) {

        public DataQuality {
            if (travelCheckPassed && warning != null) {
                throw new IllegalArgumentException("A passing travel check cannot have a warning");
            }
            if (!travelCheckPassed) {
                warning = requireText(warning, "Data quality warning");
            }
        }
    }

    public record Capabilities(
            boolean antiSquat,
            boolean antiRise,
            boolean cogAwareKickback,
            boolean referenceOnly
    ) {
    }

    /** How the analysis was calculated: what the reading has to be referred to. */
    public record Conditions(
            String bikeCategory,
            Double sagPercent,
            Integer chainringTeeth,
            Integer sprocketTeeth
    ) {
    }

    /**
     * Shape of the leverage curve, read as a whole and by thirds. A figure alone does not
     * describe a bike: the same useful progression feels different depending on where the
     * curve falls and whether any third rises.
     */
    public record LeverageShape(
            String progressionBand,
            String initialTrend,
            String middleTrend,
            String finalTrend,
            Double leverageRatioInitial,
            Double leverageRatioAtSag,
            Double leverageRatioFinal
    ) {
    }

    public record Evidence(String key, double value, String unit) {

        public Evidence {
            key = requireText(key, "Evidence key");
            unit = requireText(unit, "Evidence unit");
            if (!Double.isFinite(value)) {
                throw new IllegalArgumentException("Evidence value must be finite");
            }
        }
    }

    private static String requireText(String value, String name) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(name + " is required");
        }
        return value;
    }
}

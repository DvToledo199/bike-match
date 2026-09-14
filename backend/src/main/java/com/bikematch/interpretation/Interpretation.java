package com.bikematch.interpretation;

import java.util.List;
import java.util.Objects;

public record Interpretation(
        String summary,
        Source source,
        String providerVersion,
        List<InterpretationContext.Evidence> evidence
) {

    public Interpretation {
        summary = requireText(summary, "Interpretation summary");
        source = Objects.requireNonNull(source, "Interpretation source is required");
        providerVersion = requireText(providerVersion, "Provider version");
        evidence = List.copyOf(Objects.requireNonNull(evidence, "Interpretation evidence is required"));
        if (evidence.size() < 2 || evidence.size() > 4) {
            throw new IllegalArgumentException("Interpretation evidence must contain between 2 and 4 items");
        }
    }

    public enum Source {
        RULES,
        AI
    }

    private static String requireText(String value, String name) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(name + " is required");
        }
        return value;
    }
}

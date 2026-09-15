package com.bikematch.interpretation;

import java.util.List;
import java.util.Locale;
import org.springframework.stereotype.Component;

/**
 * Deterministic fallback used before an external AI provider is enabled.
 * It is also the safety net when an external provider is unavailable.
 */
@Component
public class RulesBasedInterpretationProvider implements InterpretationProvider {

    private static final String PROVIDER_VERSION = "rules-2";
    private static final String PROMPT_VERSION = "interpretation-prompt-1";

    @Override
    public String providerVersion() {
        return PROVIDER_VERSION;
    }

    @Override
    public String promptVersion() {
        return PROMPT_VERSION;
    }

    @Override
    public Interpretation generate(InterpretationContext context) {
        if (!context.dataQuality().travelCheckPassed()) {
            return new Interpretation(
                    "The calculated travel does not match the declared travel. Review the photo marks and calibration before drawing conclusions; the curves remain available as a reference.",
                    Interpretation.Source.RULES,
                    PROVIDER_VERSION,
                    context.evidence()
            );
        }

        StringBuilder summary = new StringBuilder("This saved analysis shows ");
        appendLeverage(summary, context);
        appendAxlePath(summary, context);
        summary.append('.');
        appendReferenceMetrics(summary, context);
        summary.append(" These are geometric tendencies from a marked photo, not a personal setup recommendation or a laboratory measurement.");

        return new Interpretation(
                summary.toString(),
                Interpretation.Source.RULES,
                PROVIDER_VERSION,
                context.evidence()
        );
    }

    private void appendLeverage(StringBuilder summary, InterpretationContext context) {
        EvidenceValue progression = evidence(context, "usefulProgressionPercent");
        if (progression == null) {
            summary.append("a suspension response that should be read together with its leverage curve");
            return;
        }
        String tendency = progression.value() >= 5 ? "a progressive leverage response" : "a mostly linear leverage response";
        summary.append(tendency)
                .append(" (useful progression: ")
                .append(format(progression.value()))
                .append(progression.unit())
                .append(")");
    }

    private void appendAxlePath(StringBuilder summary, InterpretationContext context) {
        EvidenceValue rearward = evidence(context, "maxRearwardMm");
        if (rearward != null) {
            summary.append(" and rear axle movement up to ")
                    .append(format(rearward.value()))
                    .append(rearward.unit())
                    .append(" rearward");
        }
    }

    private void appendReferenceMetrics(StringBuilder summary, InterpretationContext context) {
        if (context.capabilities().antiSquat() || context.capabilities().antiRise()) {
            summary.append(" The anti-squat and anti-rise figures are reference-model estimates.");
        }
    }

    private EvidenceValue evidence(InterpretationContext context, String key) {
        return context.evidence().stream()
                .filter(item -> item.key().equals(key))
                .findFirst()
                .map(item -> new EvidenceValue(item.value(), item.unit()))
                .orElse(null);
    }

    private String format(double value) {
        return String.format(Locale.ROOT, "%.1f", value).replaceAll("\\.0$", "");
    }

    private record EvidenceValue(double value, String unit) {
    }
}

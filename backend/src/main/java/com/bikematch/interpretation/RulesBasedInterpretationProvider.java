package com.bikematch.interpretation;

import java.util.List;
import java.util.Locale;
import org.springframework.stereotype.Component;

/**
 * Deterministic fallback used before an external AI provider is enabled.
 * It is also the safety net when an external provider is unavailable.
 *
 * <p>It writes with the same vocabulary as the AI provider, from the bands the context
 * carries: see {@code docs/base-conocimiento-cinematica.md}. It never recommends a spring,
 * and it stays silent about the axle path when the band says the figure is not worth a
 * sentence or falls outside what the chain model represents.
 */
@Component
public class RulesBasedInterpretationProvider implements InterpretationProvider {

    private static final String PROVIDER_VERSION = "rules-3";
    private static final String PROMPT_VERSION = "interpretation-prompt-1";

    /** The context offers a menu of figures; a stored explanation may cite at most four. */
    private static final int MAX_CITED_EVIDENCE = 4;

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
                    citedEvidence(context)
            );
        }

        StringBuilder summary = new StringBuilder("This saved analysis shows ");
        appendLeverage(summary, context);
        summary.append('.');
        appendKickback(summary, context);
        appendBraking(summary, context);
        appendAxlePath(summary, context);
        summary.append(" These are geometric tendencies from a marked photo, not a personal setup recommendation or a laboratory measurement.");

        return new Interpretation(
                summary.toString(),
                Interpretation.Source.RULES,
                PROVIDER_VERSION,
                citedEvidence(context)
        );
    }

    private List<InterpretationContext.Evidence> citedEvidence(InterpretationContext context) {
        List<InterpretationContext.Evidence> evidence = context.evidence();
        return evidence.subList(0, Math.min(MAX_CITED_EVIDENCE, evidence.size()));
    }

    private void appendLeverage(StringBuilder summary, InterpretationContext context) {
        EvidenceValue progression = evidence(context, "usefulProgressionPercent");
        if (progression == null) {
            summary.append("a suspension response that should be read together with its leverage curve");
            return;
        }
        summary.append(character(context.readings().progression()))
                .append(" (useful progression: ")
                .append(format(progression.value()))
                .append(progression.unit())
                .append(")");
        String bottomOut = context.leverageShape() == null ? null : context.leverageShape().bottomOutTrend();
        if ("REGRESSIVE".equals(bottomOut)) {
            summary.append(", and the leverage rises again over the last stretch, where the support is most needed");
        }
    }

    /** The band names come from the knowledge base; high progression is not the same as pop. */
    private String character(String progressionBand) {
        if (progressionBand == null) {
            return "a leverage response to be read together with its curve";
        }
        return switch (progressionBand) {
            case "REGRESSIVE" -> "a regressive leverage response, losing support as it compresses";
            case "LINEAR" -> "a linear leverage response, predictable but with little reserve of its own";
            case "SLIGHTLY_PROGRESSIVE" -> "a slightly progressive leverage response";
            case "MEDIUM" -> "a progressive leverage response with clear reserve at the end";
            case "HIGH" -> "a strongly progressive leverage response, with growing support";
            case "VERY_HIGH" -> "a very progressive leverage response, with a wide margin against hits that would use up the travel at once";
            default -> "a leverage response to be read together with its curve";
        };
    }

    /** Kickback is the chain fighting the suspension, not a measure of pedalling efficiency. */
    private void appendKickback(StringBuilder summary, InterpretationContext context) {
        EvidenceValue kickback = evidence(context, "maxKickbackDegrees");
        String band = context.readings().kickback();
        if (kickback == null || band == null) {
            return;
        }
        summary.append(" Pedal kickback reaches ")
                .append(format(kickback.value()))
                .append(kickback.unit())
                .append(gear(context))
                .append(switch (band) {
                    case "LOW" -> ": the chain barely fights the suspension, so the rear wheel keeps following the ground while you pedal.";
                    case "MEDIUM" -> ": the chain fights the suspension enough to notice on broken climbs.";
                    case "HIGH", "VERY_HIGH" -> ": the chain holds the bike extended while you pedal, so it feels firm but the rear wheel follows the ground worse on broken climbs.";
                    default -> ".";
                });
    }

    private String gear(InterpretationContext context) {
        InterpretationContext.Conditions conditions = context.conditions();
        if (conditions == null || conditions.chainringTeeth() == null || conditions.sprocketTeeth() == null) {
            return "";
        }
        return " in " + conditions.chainringTeeth() + "x" + conditions.sprocketTeeth();
    }

    private void appendBraking(StringBuilder summary, InterpretationContext context) {
        String band = context.readings().antiRise();
        if (band == null) {
            return;
        }
        summary.append(switch (band) {
            case "EXTENDS_UNDER_BRAKING" -> " Under braking the rear tends to extend, so the suspension stays free and follows the ground well, at the cost of more pitching.";
            case "BALANCED" -> " Under braking it sits in the balance most modern bikes aim for.";
            case "SQUATS_UNDER_BRAKING" -> " Under braking the bike squats at the rear: steadier on steep ground, but it copies the surface less well.";
            case "SITS_HARD_UNDER_BRAKING" -> " Under braking the bike clearly sits down at the rear.";
            default -> "";
        });
    }

    private void appendAxlePath(StringBuilder summary, InterpretationContext context) {
        EvidenceValue rearward = evidence(context, "maxRearwardMm");
        String band = context.readings().axlePath();
        if (rearward == null || !("SLIGHT".equals(band) || "NOTICEABLE".equals(band))) {
            return;
        }
        summary.append(" The rear axle moves up to ")
                .append(format(rearward.value()))
                .append(rearward.unit())
                .append(" rearward, which helps it swallow square edges.");
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

package com.bikematch.interpretation;

import java.util.List;
import java.util.Locale;
import org.springframework.stereotype.Component;

/**
 * Deterministic fallback used before an external AI provider is enabled.
 * It is also the safety net when an external provider is unavailable.
 *
 * <p>It writes with the same vocabulary as the AI provider, from the bands the context
 * carries: see {@code docs/base-conocimiento-cinematica.md}. No bike is made to sound bad,
 * no spring is recommended, and the gear the figures were calculated in is not printed: it
 * means nothing to the reader.
 */
@Component
public class RulesBasedInterpretationProvider implements InterpretationProvider {

    private static final String PROVIDER_VERSION = "rules-4";
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
        appendPedalling(summary, context);
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

    /** Progression over the whole travel: the one percentage riders actually read. */
    private void appendLeverage(StringBuilder summary, InterpretationContext context) {
        EvidenceValue progression = evidence(context, "totalProgressionPercent");
        if (progression == null) {
            summary.append("a suspension response that should be read together with its leverage curve");
            return;
        }
        summary.append(character(context.readings().progression()))
                .append(" (progression: ")
                .append(format(progression.value()))
                .append(progression.unit())
                .append(")");
        String bottomOut = context.leverageShape() == null ? null : context.leverageShape().bottomOutTrend();
        if ("REGRESSIVE".equals(bottomOut)) {
            summary.append(", with the leverage rising again over the last stretch of travel");
        }
    }

    /** Linear means predictable, and nothing more; high progression does not imply pop. */
    private String character(String progressionBand) {
        if (progressionBand == null) {
            return "a leverage response to be read together with its curve";
        }
        return switch (progressionBand) {
            case "REGRESSIVE" -> "a regressive leverage response, which asks more of the shock at the end of the travel";
            case "LINEAR" -> "a linear leverage response, so the suspension behaves predictably";
            case "SLIGHTLY_PROGRESSIVE" -> "a slightly progressive leverage response";
            case "MEDIUM" -> "a progressive leverage response";
            case "HIGH" -> "a strongly progressive leverage response, with growing support";
            case "VERY_HIGH" -> "a very progressive leverage response, with a wide margin against hits that would use up the travel at once";
            default -> "a leverage response to be read together with its curve";
        };
    }

    /**
     * Pedalling in one sentence. Chain tension and anti-squat describe the same thing to the
     * rider: whether the effort goes into moving forward or into the shock.
     */
    private void appendPedalling(StringBuilder summary, InterpretationContext context) {
        String band = context.readings().antiSquat();
        if (band == null) {
            return;
        }
        summary.append(switch (band) {
            case "SOFT" -> " Pedalling, the shock takes up part of your effort, so on smooth climbs you will reach for the lockout more often.";
            case "BALANCED" -> " It pedals in balance: part of your effort reaches the shock, but not enough to get in the way.";
            case "FIRM", "VERY_FIRM" -> " It pedals efficiently: the chain keeps the bike extended, so your effort goes into moving forward rather than into the shock.";
            case "EXTREME" -> " It pedals very firmly, with almost none of your effort reaching the shock.";
            default -> "";
        });
    }

    /** Said the way the rider feels it: a rear that lifts, or one that settles. */
    private void appendBraking(StringBuilder summary, InterpretationContext context) {
        String band = context.readings().antiRise();
        if (band == null) {
            return;
        }
        summary.append(switch (band) {
            case "EXTENDS_UNDER_BRAKING" -> " Braking, the rear lifts a little, so the bike feels less settled and the slope feels steeper, while the shock copies the ground better.";
            case "BALANCED" -> " Braking, it sits in the balance most modern bikes aim for.";
            case "SQUATS_UNDER_BRAKING" -> " Braking, the rear settles, which makes the bike more stable, as a consequence of not copying the ground as closely.";
            case "SITS_HARD_UNDER_BRAKING" -> " Braking, the rear settles clearly, which makes the bike very stable, as a consequence of copying the ground less closely.";
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

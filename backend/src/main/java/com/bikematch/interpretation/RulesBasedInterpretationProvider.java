package com.bikematch.interpretation;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import org.springframework.context.MessageSource;
import org.springframework.stereotype.Component;

/**
 * Deterministic fallback used before an external AI provider is enabled.
 * It is also the safety net when an external provider is unavailable.
 *
 * <p>It writes with the same vocabulary as the AI provider, from the bands the context
 * carries: see {@code docs/base-conocimiento-cinematica.md}. No bike is made to sound bad,
 * no spring is recommended, and the gear the figures were calculated in is not printed: it
 * means nothing to the reader.
 *
 * <p>The sentences live in {@code messages.properties} (English) and
 * {@code messages_es.properties} (Spanish). This class only chooses which ones apply;
 * Spring's MessageSource returns them in the language of the context.
 */
@Component
public class RulesBasedInterpretationProvider implements InterpretationProvider {

    private static final String PROVIDER_VERSION = "rules-4";
    private static final String PROMPT_VERSION = "interpretation-prompt-1";

    /** The context offers a menu of figures; a stored explanation may cite at most four. */
    private static final int MAX_CITED_EVIDENCE = 4;

    private final MessageSource messages;

    public RulesBasedInterpretationProvider(MessageSource messages) {
        this.messages = messages;
    }

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
        Locale locale = Locale.forLanguageTag(context.language());
        if (!context.dataQuality().travelCheckPassed()) {
            return new Interpretation(
                    text("rules.travelMismatch", locale),
                    Interpretation.Source.RULES,
                    PROVIDER_VERSION,
                    citedEvidence(context)
            );
        }

        List<String> sentences = new ArrayList<>();
        sentences.add(text("rules.opening", locale, leverage(context, locale)));
        optionalText("rules.pedalling.", context.readings().antiSquat(), locale).ifPresent(sentences::add);
        optionalText("rules.braking.", context.readings().antiRise(), locale).ifPresent(sentences::add);
        axlePath(context, locale).ifPresent(sentences::add);
        sentences.add(text("rules.closing", locale));

        return new Interpretation(
                String.join(" ", sentences),
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
    private String leverage(InterpretationContext context, Locale locale) {
        EvidenceValue progression = evidence(context, "totalProgressionPercent");
        if (progression == null) {
            return text("rules.leverage.withoutProgression", locale);
        }
        String leverage = character(context.readings().progression(), locale) + " "
                + text("rules.leverage.progression", locale, format(progression.value()) + progression.unit());
        String bottomOut = context.leverageShape() == null ? null : context.leverageShape().bottomOutTrend();
        if ("REGRESSIVE".equals(bottomOut)) {
            leverage += text("rules.leverage.bottomOutRising", locale);
        }
        return leverage;
    }

    private String character(String progressionBand, Locale locale) {
        return optionalText("rules.character.", progressionBand, locale)
                .orElseGet(() -> text("rules.character.unknown", locale));
    }

    /** Only an axle path worth a sentence gets one. */
    private Optional<String> axlePath(InterpretationContext context, Locale locale) {
        EvidenceValue rearward = evidence(context, "maxRearwardMm");
        String band = context.readings().axlePath();
        if (rearward == null || !("SLIGHT".equals(band) || "NOTICEABLE".equals(band))) {
            return Optional.empty();
        }
        return Optional.of(text("rules.axlePath", locale, format(rearward.value()) + rearward.unit()));
    }

    private String text(String key, Locale locale, Object... figures) {
        return messages.getMessage(key, figures, locale);
    }

    /** A band without a sentence of its own, or no band at all, adds nothing. */
    private Optional<String> optionalText(String keyPrefix, String band, Locale locale) {
        if (band == null) {
            return Optional.empty();
        }
        return Optional.ofNullable(messages.getMessage(keyPrefix + band, null, null, locale));
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

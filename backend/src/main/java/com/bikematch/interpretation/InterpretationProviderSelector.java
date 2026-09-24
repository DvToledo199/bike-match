package com.bikematch.interpretation;

import java.util.Set;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class InterpretationProviderSelector {

    /** The values INTERPRETATION_PROVIDER accepts; anything else would quietly mean rules. */
    private static final Set<String> KNOWN_PROVIDERS = Set.of("rules", "google-genai");

    private final RulesBasedInterpretationProvider rulesProvider;
    private final ObjectProvider<GeminiInterpretationProvider> geminiProvider;

    public InterpretationProviderSelector(
            RulesBasedInterpretationProvider rulesProvider,
            ObjectProvider<GeminiInterpretationProvider> geminiProvider,
            @Value("${spring.ai.model.chat:rules}") String configuredProvider
    ) {
        if (!KNOWN_PROVIDERS.contains(configuredProvider)) {
            throw new IllegalStateException("INTERPRETATION_PROVIDER must be rules or google-genai, not '"
                    + configuredProvider + "'");
        }
        this.rulesProvider = rulesProvider;
        this.geminiProvider = geminiProvider;
    }

    public InterpretationProvider current() {
        GeminiInterpretationProvider configuredGemini = geminiProvider.getIfAvailable();
        return configuredGemini == null ? rulesProvider : configuredGemini;
    }

    public InterpretationProvider fallback() {
        return rulesProvider;
    }
}

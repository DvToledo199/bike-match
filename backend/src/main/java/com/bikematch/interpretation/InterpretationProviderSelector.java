package com.bikematch.interpretation;

import java.util.Optional;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Component;

@Component
public class InterpretationProviderSelector {

    private final RulesBasedInterpretationProvider rulesProvider;
    private final ObjectProvider<GeminiInterpretationProvider> geminiProvider;

    public InterpretationProviderSelector(
            RulesBasedInterpretationProvider rulesProvider,
            ObjectProvider<GeminiInterpretationProvider> geminiProvider
    ) {
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

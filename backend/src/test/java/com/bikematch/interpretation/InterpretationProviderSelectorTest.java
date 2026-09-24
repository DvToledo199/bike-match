package com.bikematch.interpretation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.ObjectProvider;

@ExtendWith(MockitoExtension.class)
class InterpretationProviderSelectorTest {

    @Mock private RulesBasedInterpretationProvider rulesProvider;
    @Mock private ObjectProvider<GeminiInterpretationProvider> geminiProvider;

    @Test
    void rulesAreUsedWhenGeminiIsNotConfigured() {
        given(geminiProvider.getIfAvailable()).willReturn(null);

        var selector = new InterpretationProviderSelector(rulesProvider, geminiProvider, "rules");

        assertThat(selector.current()).isSameAs(rulesProvider);
    }

    /** A mistyped or outdated value, such as the former "gemini", would otherwise quietly mean rules. */
    @Test
    void unknownProviderStopsTheApplicationAtStartup() {
        assertThatThrownBy(() -> new InterpretationProviderSelector(rulesProvider, geminiProvider, "gemini"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("rules or google-genai");
    }
}

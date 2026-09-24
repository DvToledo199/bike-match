package com.bikematch.interpretation;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.Properties;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.autoconfigure.context.MessageSourceAutoConfiguration;
import org.springframework.boot.test.context.ConfigDataApplicationContextInitializer;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.MessageSource;

/** The rules texts come in two files; these checks keep them usable as a pair. */
class RulesMessagesTest {

    @Test
    void spanishHasExactlyTheEnglishKeys() throws IOException {
        assertThat(load("messages_es.properties").stringPropertyNames())
                .isEqualTo(load("messages.properties").stringPropertyNames());
    }

    /** A server set to Spanish must still answer an English request in English. */
    @Test
    void englishStaysEnglishOnAServerSetToSpanish() {
        Locale machineLocale = Locale.getDefault();
        Locale.setDefault(Locale.forLanguageTag("es-ES"));
        try {
            new ApplicationContextRunner()
                    .withInitializer(new ConfigDataApplicationContextInitializer())
                    .withConfiguration(AutoConfigurations.of(MessageSourceAutoConfiguration.class))
                    .run(context -> {
                        MessageSource messages = context.getBean(MessageSource.class);
                        assertThat(messages.getMessage("rules.closing", null, Locale.ENGLISH))
                                .startsWith("These are geometric tendencies");
                        assertThat(messages.getMessage("rules.closing", null, Locale.forLanguageTag("es")))
                                .startsWith("Son tendencias geométricas");
                    });
        } finally {
            Locale.setDefault(machineLocale);
        }
    }

    private Properties load(String name) throws IOException {
        Properties properties = new Properties();
        try (var reader = new InputStreamReader(
                getClass().getResourceAsStream("/" + name), StandardCharsets.UTF_8)) {
            properties.load(reader);
        }
        return properties;
    }
}

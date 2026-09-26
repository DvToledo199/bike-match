package com.bikematch.interpretation;

import static org.assertj.core.api.Assertions.assertThat;

import com.google.genai.Client;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.google.genai.GoogleGenAiChatModel;
import org.springframework.ai.google.genai.GoogleGenAiChatOptions;
import org.springframework.ai.model.chat.client.autoconfigure.ChatClientAutoConfiguration;
import org.springframework.ai.model.google.genai.autoconfigure.chat.GoogleGenAiChatAutoConfiguration;
import org.springframework.ai.model.tool.autoconfigure.ToolCallingAutoConfiguration;
import org.springframework.ai.retry.autoconfigure.SpringAiRetryAutoConfiguration;
import org.springframework.ai.retry.autoconfigure.SpringAiRetryProperties;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.autoconfigure.jackson.JacksonAutoConfiguration;
import org.springframework.boot.convert.ApplicationConversionService;
import org.springframework.boot.test.context.ConfigDataApplicationContextInitializer;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

/**
 * Checks the Spring AI wiring against the real application.properties, without calling Gemini:
 * building the client does not open any connection.
 */
class GeminiConfigurationTest {

    private final ApplicationContextRunner runner = new ApplicationContextRunner()
            .withInitializer(new ConfigDataApplicationContextInitializer())
            // Like a running application, read "PT30S" as a Duration.
            .withInitializer(context -> context.getBeanFactory()
                    .setConversionService(ApplicationConversionService.getSharedInstance()))
            .withConfiguration(AutoConfigurations.of(
                    JacksonAutoConfiguration.class,
                    SpringAiRetryAutoConfiguration.class,
                    ToolCallingAutoConfiguration.class,
                    GoogleGenAiChatAutoConfiguration.class,
                    ChatClientAutoConfiguration.class))
            .withUserConfiguration(GeminiClientConfig.class, GeminiInterpretationProvider.class);

    @Test
    void rulesModeBuildsNoGeminiClientSoItNeedsNoKey() {
        runner.withPropertyValues("spring.ai.model.chat=rules", "spring.ai.google.genai.api-key=")
                .run(context -> {
                    assertThat(context).hasNotFailed();
                    assertThat(context).doesNotHaveBean(Client.class);
                    assertThat(context).doesNotHaveBean(ChatModel.class);
                    assertThat(context).doesNotHaveBean(GeminiInterpretationProvider.class);
                });
    }

    @Test
    void geminiModeUsesTheModelAsConfiguredWithOneAttemptAndOurTimedClient() {
        runner.withPropertyValues(
                        "spring.ai.model.chat=google-genai",
                        "spring.ai.google.genai.api-key=test-key",
                        "spring.ai.google.genai.chat.options.model=test-model")
                .run(context -> {
                    assertThat(context).hasSingleBean(GeminiInterpretationProvider.class);
                    assertThat(context.getBeanFactory().getBeanDefinition("googleGenAiClient")
                            .getFactoryBeanName()).containsIgnoringCase("geminiClientConfig");

                    GoogleGenAiChatOptions options = (GoogleGenAiChatOptions)
                            context.getBean(GoogleGenAiChatModel.class).getDefaultOptions();
                    assertThat(options.getModel()).isEqualTo("test-model");
                    assertThat(options.getResponseMimeType()).isEqualTo("application/json");
                    assertThat(options.getTemperature()).isEqualTo(1.0);
                    assertThat(context.getBean(SpringAiRetryProperties.class).getMaxAttempts()).isEqualTo(1);
                });
    }
}

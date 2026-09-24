package com.bikematch.interpretation;

import com.google.genai.Client;
import com.google.genai.types.HttpOptions;
import com.google.genai.types.HttpRetryOptions;
import java.time.Duration;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * The one piece of the Gemini setup that Spring AI's auto-configuration does not cover.
 * <p>
 * The Google client, left to its defaults, has no time limit at all and silently retries up to
 * five times when Gemini is busy or the quota is used up, waiting between attempts. This bean
 * replaces the auto-configured client with the same one, limited in time and to a single attempt:
 * a failure falls back to the rules text straight away. The model and the JSON format still come
 * from the {@code spring.ai.*} properties.
 */
@Configuration(proxyBeanMethods = false)
@ConditionalOnProperty(name = "spring.ai.model.chat", havingValue = "google-genai")
public class GeminiClientConfig {

    @Bean
    Client googleGenAiClient(
            @Value("${spring.ai.google.genai.api-key:}") String apiKey,
            @Value("${app.interpretation.gemini.timeout:PT60S}") Duration timeout
    ) {
        if (apiKey.isBlank()) {
            throw new IllegalStateException("INTERPRETATION_PROVIDER=google-genai requires GEMINI_API_KEY");
        }
        return Client.builder()
                .apiKey(apiKey)
                .httpOptions(HttpOptions.builder()
                        .timeout(Math.toIntExact(timeout.toMillis()))
                        .retryOptions(HttpRetryOptions.builder().attempts(1).build())
                        .build())
                .build();
    }
}

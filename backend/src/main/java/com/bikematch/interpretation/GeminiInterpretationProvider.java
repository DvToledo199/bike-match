package com.bikematch.interpretation;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.time.Duration;
import java.util.HashSet;
import java.util.Set;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

/**
 * Gemini adapter. It is only created when the application explicitly selects it.
 * The rest of the application talks to the InterpretationProvider interface.
 */
@Component
@ConditionalOnProperty(name = "app.interpretation.provider", havingValue = "gemini")
public class GeminiInterpretationProvider implements InterpretationProvider {

    private static final String SYSTEM_INSTRUCTION = """
            You are a mountain-bike suspension specialist writing for a rider who is not an
            engineer. Interpret the bike; never just list its numbers.

            Write the summary as flowing plain text, in this order:
            1. One opening sentence with the character of the bike: read leverageShape, say whether
               it is linear, progressive or regressive and what that means when riding it.
            2. The two or three figures that support it, each with its reading.
            3. The trade-off: what the design gains and what it pays for it.
            4. Which spring suits it in general terms: coil or air, chamber volume, volume spacers.
            5. Who it fits and who it does not, in general terms.
            6. One short closing sentence keeping the reader honest about what this analysis is.

            Rules:
            - Use only values present in the JSON context. Never invent a number or a metric, and
              never mention a metric that is missing. Respect forbiddenTopics.
            - No brands, no product models, no pressures, no click counts, no guarantees, and
              nothing that assumes this rider's weight or setup.
            - Write figures with the % symbol and at most one decimal.
            - Speak in tendencies: the points come from a hand-marked photo.
            - Do not inflate small differences. With maxRearwardMm under 3, call the axle path
              conventional with no perceptible effect instead of building a story on it.
            - Cite the conditions that a figure depends on: the sag percentage for the curve, the
              chainring and sprocket for kickback.
            - If dataQuality.travelCheckPassed is false, lead with that warning and draw no firm
              conclusion.
            - Keep the whole summary under 120 words.

            Return exactly JSON with this shape: {\"summary\":\"...\",\"evidenceKeys\":[\"...\"]}.
            Use between two and four evidenceKeys taken from the context evidence, the ones you
            actually cited. Plain text only, no Markdown and no HTML.
            """;
    private static final String PROMPT_VERSION = "interpretation-prompt-2";

    private final RestClient restClient;
    private final ObjectMapper objectMapper;
    private final String apiKey;
    private final String model;

    public GeminiInterpretationProvider(
            ObjectMapper objectMapper,
            @Value("${app.interpretation.gemini.api-key:}") String apiKey,
            @Value("${app.interpretation.gemini.model:}") String model,
            @Value("${app.interpretation.gemini.base-url:https://generativelanguage.googleapis.com}") String baseUrl,
            @Value("${app.interpretation.gemini.timeout:PT15S}") Duration timeout
    ) {
        this.objectMapper = objectMapper;
        this.apiKey = apiKey;
        this.model = model;
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(timeout);
        requestFactory.setReadTimeout(timeout);
        this.restClient = RestClient.builder()
                .baseUrl(baseUrl)
                .requestFactory(requestFactory)
                .build();
    }

    @Override
    public String providerVersion() {
        return "gemini-" + model;
    }

    @Override
    public String promptVersion() {
        return PROMPT_VERSION;
    }

    @Override
    public Interpretation generate(InterpretationContext context) {
        if (apiKey.isBlank() || model.isBlank()) {
            throw new InterpretationProviderException(
                    "Gemini provider requires GEMINI_API_KEY and GEMINI_MODEL");
        }

        try {
            JsonNode response = restClient.post()
                    .uri("/v1beta/models/{model}:generateContent", model)
                    .header("x-goog-api-key", apiKey)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(requestFor(context))
                    .retrieve()
                    .body(JsonNode.class);
            return parseResponse(response, context);
        } catch (RestClientException | JsonProcessingException exception) {
            throw new InterpretationProviderException("Gemini did not return a valid explanation", exception);
        }
    }

    private ObjectNode requestFor(InterpretationContext context) throws JsonProcessingException {
        ObjectNode request = objectMapper.createObjectNode();
        ObjectNode systemInstruction = request.putObject("system_instruction");
        systemInstruction.putArray("parts").addObject().put("text", SYSTEM_INSTRUCTION);

        ObjectNode userContent = request.putArray("contents").addObject();
        userContent.put("role", "user");
        userContent.putArray("parts").addObject()
                .put("text", objectMapper.writeValueAsString(context));

        request.putObject("generationConfig").put("responseMimeType", "application/json");
        return request;
    }

    private Interpretation parseResponse(JsonNode response, InterpretationContext context)
            throws JsonProcessingException {
        String rawText = response == null
                ? ""
                : response.at("/candidates/0/content/parts/0/text").asText("");
        if (rawText.isBlank()) {
            throw new InterpretationProviderException("Gemini returned no explanation text");
        }

        JsonNode generated = objectMapper.readTree(rawText);
        String summary = generated.path("summary").asText("").trim();
        JsonNode evidenceKeys = generated.path("evidenceKeys");
        if (summary.isBlank() || summary.length() > Interpretation.MAX_SUMMARY_LENGTH
                || summary.contains("<") || summary.contains(">")
                || !evidenceKeys.isArray() || evidenceKeys.size() < 2 || evidenceKeys.size() > 4) {
            throw new InterpretationProviderException("Gemini returned an unsafe explanation shape");
        }

        Set<String> allowedEvidence = context.evidence().stream()
                .map(InterpretationContext.Evidence::key)
                .collect(java.util.stream.Collectors.toSet());
        Set<String> selectedEvidence = new HashSet<>();
        for (JsonNode evidenceKey : evidenceKeys) {
            if (!evidenceKey.isTextual() || !allowedEvidence.contains(evidenceKey.asText())) {
                throw new InterpretationProviderException("Gemini returned an unknown evidence key");
            }
            selectedEvidence.add(evidenceKey.asText());
        }

        if (selectedEvidence.size() < 2) {
            throw new InterpretationProviderException("Gemini returned duplicate evidence keys");
        }

        var evidence = context.evidence().stream()
                .filter(item -> selectedEvidence.contains(item.key()))
                .toList();
        return new Interpretation(summary, Interpretation.Source.AI, providerVersion(), evidence);
    }
}

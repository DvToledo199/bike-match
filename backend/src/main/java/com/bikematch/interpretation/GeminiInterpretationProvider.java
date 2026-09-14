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
            You explain mountain-bike suspension kinematics to beginners.
            Use only the data and allowed topics in the supplied JSON context.
            Do not invent metrics, values, rider suitability, pressures, clicks, products,
            safety guarantees or personal setup recommendations. If dataQuality.travelCheckPassed
            is false, lead with the warning and do not draw a firm conclusion.
            Return exactly JSON with this shape: {\"summary\":\"...\",\"evidenceKeys\":[\"...\"]}.
            The summary must be plain text in the requested language, with no Markdown or HTML.
            Use between two and four evidence keys from the context and keep the explanation concise.
            """;
    private static final String PROMPT_VERSION = "interpretation-prompt-1";

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

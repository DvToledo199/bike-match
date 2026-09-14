package com.bikematch.interpretation;

import com.bikematch.bike.KinematicsResult;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import org.springframework.stereotype.Component;

@Component
public class InterpretationContextFactory {

    public static final int CONTEXT_VERSION = 1;
    public static final String RULES_VERSION = "kinematics-rules-1";

    private final ObjectMapper objectMapper;

    public InterpretationContextFactory(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public InterpretationContext create(KinematicsResult result, String requestedLanguage) {
        String language = normalizeLanguage(requestedLanguage);
        JsonNode descriptors = parseObject(result.getDescriptors(), "descriptors");
        JsonNode curves = parseObject(result.getCurves(), "curves");
        JsonNode capabilitiesNode = parseObject(result.getCapabilities(), "capabilities");

        boolean antiSquat = capabilitiesNode.path("antiSquat").asBoolean(false);
        boolean antiRise = capabilitiesNode.path("antiRise").asBoolean(false);
        boolean cogAwareKickback = capabilitiesNode.path("cogAwareKickback").asBoolean(false);
        boolean referenceOnly = capabilitiesNode.path("referenceOnly").asBoolean(false);
        InterpretationContext.Capabilities capabilities = new InterpretationContext.Capabilities(
                antiSquat, antiRise, cogAwareKickback, referenceOnly);

        JsonNode travelCheck = descriptors.path("travelCheck");
        boolean travelPassed = travelCheck.path("withinTolerance").asBoolean(false);
        String warning = travelPassed
                ? null
                : "Calculated travel differs from declared travel by "
                        + format(travelCheck.path("deviationPercent").asDouble(0)) + "%";

        List<InterpretationContext.Evidence> evidence = evidenceFrom(descriptors, curves);
        List<String> allowedTopics = new ArrayList<>(List.of("leverage", "axlePath", "kickback"));
        if (antiSquat) allowedTopics.add("antiSquat");
        if (antiRise) allowedTopics.add("antiRise");

        List<String> forbiddenTopics = new ArrayList<>(
                List.of("pressure", "clicks", "productModels", "riderSuitability"));
        if (!antiSquat) forbiddenTopics.add("antiSquat");
        if (!antiRise) forbiddenTopics.add("antiRise");
        if (!cogAwareKickback) forbiddenTopics.add("sprocketInfluence");

        List<String> limits = new ArrayList<>(List.of(
                "Marked-photo geometry; not a laboratory measurement.",
                "The complete behaviour also depends on the shock, setup and rider."));
        if (referenceOnly) {
            limits.add("Reference wheel radii and centre of gravity; not user measurements.");
        }

        return new InterpretationContext(
                CONTEXT_VERSION,
                result.getResultVersion(),
                result.getEngineVersion(),
                RULES_VERSION,
                language,
                new InterpretationContext.DataQuality(travelPassed, warning),
                capabilities,
                evidence,
                limits,
                allowedTopics,
                forbiddenTopics);
    }

    private List<InterpretationContext.Evidence> evidenceFrom(JsonNode descriptors, JsonNode curves) {
        List<InterpretationContext.Evidence> evidence = new ArrayList<>();
        addNumber(evidence, "usefulProgressionPercent", "%",
                descriptors.path("leverageDescriptors").path("usefulProgressionPercent"));
        addNumber(evidence, "maxRearwardMm", "mm",
                descriptors.path("axlePathDescriptors").path("maxRearwardMm"));

        JsonNode kickbackCurve = curves.path("kickbackCurve");
        if (kickbackCurve.isArray() && !kickbackCurve.isEmpty()) {
            double maximum = 0;
            for (JsonNode sample : kickbackCurve) {
                maximum = Math.max(maximum, Math.abs(sample.path("kickbackDegrees").asDouble(0)));
            }
            evidence.add(new InterpretationContext.Evidence("maxKickbackDegrees", maximum, "°"));
        }

        addNumber(evidence, "calculatedTravelMm", "mm",
                descriptors.path("travelCheck").path("calculatedTravelMm"));
        addNumber(evidence, "leverageRatioAtSag", "ratio",
                descriptors.path("leverageDescriptors").path("lrAtSag"));

        if (evidence.size() < 2) {
            throw new InterpretationProviderException(
                    "Stored kinematics result does not contain enough interpretation evidence");
        }
        return evidence.subList(0, Math.min(4, evidence.size()));
    }

    private void addNumber(List<InterpretationContext.Evidence> evidence,
                           String key, String unit, JsonNode node) {
        if (node.isNumber() && Double.isFinite(node.asDouble())) {
            evidence.add(new InterpretationContext.Evidence(key, node.asDouble(), unit));
        }
    }

    private JsonNode parseObject(String json, String fieldName) {
        try {
            JsonNode node = objectMapper.readTree(json);
            if (node == null || !node.isObject()) {
                throw new InterpretationProviderException(
                        "Stored kinematics " + fieldName + " must be a JSON object");
            }
            return node;
        } catch (JsonProcessingException exception) {
            throw new InterpretationProviderException(
                    "Stored kinematics " + fieldName + " is not valid JSON", exception);
        }
    }

    private String normalizeLanguage(String requestedLanguage) {
        String language = requestedLanguage == null ? "en" : requestedLanguage.trim().toLowerCase(Locale.ROOT);
        if (!language.equals("en")) {
            throw new IllegalArgumentException("Only the English interpretation is available yet");
        }
        return language;
    }

    private String format(double value) {
        return String.format(Locale.ROOT, "%.1f", value).replaceAll("\\.0$", "");
    }
}

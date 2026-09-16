package com.bikematch.interpretation;

import com.bikematch.bike.BikeCategory;
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

    public static final int CONTEXT_VERSION = 2;
    public static final String RULES_VERSION = "kinematics-rules-1";

    private final ObjectMapper objectMapper;

    public InterpretationContextFactory(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public InterpretationContext create(
            KinematicsResult result, String requestedLanguage, BikeCategory category) {
        String language = normalizeLanguage(requestedLanguage);
        JsonNode descriptors = parseObject(result.getDescriptors(), "descriptors");
        JsonNode curves = parseObject(result.getCurves(), "curves");
        JsonNode capabilitiesNode = parseObject(result.getCapabilities(), "capabilities");
        JsonNode leverage = descriptors.path("leverageDescriptors");
        JsonNode conditionsNode = descriptors.path("conditions");
        JsonNode travelCheck = descriptors.path("travelCheck");

        boolean antiSquat = capabilitiesNode.path("antiSquat").asBoolean(false);
        boolean antiRise = capabilitiesNode.path("antiRise").asBoolean(false);
        boolean cogAwareKickback = capabilitiesNode.path("cogAwareKickback").asBoolean(false);
        boolean referenceOnly = capabilitiesNode.path("referenceOnly").asBoolean(false);
        InterpretationContext.Capabilities capabilities = new InterpretationContext.Capabilities(
                antiSquat, antiRise, cogAwareKickback, referenceOnly);

        boolean travelPassed = travelCheck.path("withinTolerance").asBoolean(false);
        String warning = travelPassed
                ? null
                : "Calculated travel differs from declared travel by "
                        + format(travelCheck.path("deviationPercent").asDouble(0)) + "%";

        InterpretationContext.Conditions conditions = new InterpretationContext.Conditions(
                category == null ? null : category.name(),
                number(conditionsNode.path("sagPercent")),
                integer(conditionsNode.path("chainringTeeth")),
                integer(conditionsNode.path("sprocketTeeth")));

        InterpretationContext.LeverageShape leverageShape = new InterpretationContext.LeverageShape(
                text(leverage.path("progressionBand")),
                text(leverage.path("initialTrend")),
                text(leverage.path("middleTrend")),
                text(leverage.path("finalTrend")),
                number(leverage.path("lrInitial")),
                number(leverage.path("lrAtSag")),
                number(leverage.path("lrFinal")));

        List<InterpretationContext.Evidence> evidence =
                evidenceFrom(descriptors, curves, capabilities, sagTravelMm(descriptors));

        List<String> allowedTopics = new ArrayList<>(List.of(
                "leverage", "axlePath", "kickback", "springType", "volumeSpacers", "riderFit"));
        if (antiSquat) allowedTopics.add("antiSquat");
        if (antiRise) allowedTopics.add("antiRise");

        // Generic spring and rider-fit guidance is wanted; anything that pretends to know the
        // rider's own setup is not.
        List<String> forbiddenTopics = new ArrayList<>(
                List.of("pressure", "clicks", "productModels", "brands", "guarantees"));
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
                conditions,
                leverageShape,
                evidence,
                limits,
                allowedTopics,
                forbiddenTopics);
    }

    /**
     * The figures a provider may cite, most telling first. Anti-squat and anti-rise are read
     * at the sag point, which is where the knowledge base reads them.
     */
    private List<InterpretationContext.Evidence> evidenceFrom(
            JsonNode descriptors,
            JsonNode curves,
            InterpretationContext.Capabilities capabilities,
            Double sagTravelMm
    ) {
        JsonNode leverage = descriptors.path("leverageDescriptors");
        List<InterpretationContext.Evidence> evidence = new ArrayList<>();
        addNumber(evidence, "usefulProgressionPercent", "%",
                leverage.path("usefulProgressionPercent"));
        addNumber(evidence, "leverageRatioAtSag", "ratio", leverage.path("lrAtSag"));
        addNumber(evidence, "maxRearwardMm", "mm",
                descriptors.path("axlePathDescriptors").path("maxRearwardMm"));
        addMaximum(evidence, "maxKickbackDegrees", "°",
                curves.path("kickbackCurve"), "kickbackDegrees");
        if (capabilities.antiSquat()) {
            addAtSag(evidence, "antiSquatAtSagPercent", "%", curves.path("antiSquatCurve"), sagTravelMm);
        }
        if (capabilities.antiRise()) {
            addAtSag(evidence, "antiRiseAtSagPercent", "%", curves.path("antiRiseCurve"), sagTravelMm);
        }
        addNumber(evidence, "calculatedTravelMm", "mm",
                descriptors.path("travelCheck").path("calculatedTravelMm"));
        addNumber(evidence, "totalProgressionPercent", "%",
                leverage.path("totalProgressionPercent"));

        if (evidence.size() < InterpretationContext.MIN_EVIDENCE) {
            throw new InterpretationProviderException(
                    "Stored kinematics result does not contain enough interpretation evidence");
        }
        return evidence.subList(0, Math.min(InterpretationContext.MAX_EVIDENCE, evidence.size()));
    }

    private Double sagTravelMm(JsonNode descriptors) {
        Double sagPercent = number(descriptors.path("conditions").path("sagPercent"));
        Double travel = number(descriptors.path("travelCheck").path("calculatedTravelMm"));
        return sagPercent == null || travel == null ? null : travel * sagPercent / 100;
    }

    private void addAtSag(List<InterpretationContext.Evidence> evidence, String key, String unit,
                          JsonNode curve, Double sagTravelMm) {
        if (!curve.isArray() || curve.isEmpty() || sagTravelMm == null) {
            return;
        }
        JsonNode closest = null;
        double smallestDistance = Double.MAX_VALUE;
        for (JsonNode sample : curve) {
            double distance = Math.abs(sample.path("wheelTravelMm").asDouble(Double.MAX_VALUE) - sagTravelMm);
            if (distance < smallestDistance) {
                smallestDistance = distance;
                closest = sample;
            }
        }
        if (closest != null) {
            addNumber(evidence, key, unit, closest.path("percent"));
        }
    }

    private void addMaximum(List<InterpretationContext.Evidence> evidence, String key, String unit,
                            JsonNode curve, String field) {
        if (!curve.isArray() || curve.isEmpty()) {
            return;
        }
        double maximum = 0;
        for (JsonNode sample : curve) {
            maximum = Math.max(maximum, Math.abs(sample.path(field).asDouble(0)));
        }
        evidence.add(new InterpretationContext.Evidence(key, maximum, unit));
    }

    private void addNumber(List<InterpretationContext.Evidence> evidence,
                           String key, String unit, JsonNode node) {
        Double value = number(node);
        if (value != null) {
            evidence.add(new InterpretationContext.Evidence(key, value, unit));
        }
    }

    private Double number(JsonNode node) {
        return node.isNumber() && Double.isFinite(node.asDouble()) ? node.asDouble() : null;
    }

    private Integer integer(JsonNode node) {
        return node.isNumber() ? node.asInt() : null;
    }

    private String text(JsonNode node) {
        return node.isTextual() && !node.asText().isBlank() ? node.asText() : null;
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

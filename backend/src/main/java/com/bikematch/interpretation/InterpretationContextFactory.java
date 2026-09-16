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

    public static final int CONTEXT_VERSION = 4;
    public static final String RULES_VERSION = "kinematics-rules-1";

    /** Section boundaries of the travel, as fractions: initial feel, mid support, reserve. */
    private static final double INITIAL_FEEL_END = 0.40;
    private static final double MID_SUPPORT_END = 0.70;

    /** A leverage change within this over a section is flat, as the engine reads it. */
    private static final double FLAT_BAND = 0.1;

    /** Below this the axle path is not worth a sentence (base-conocimiento section 4). */
    private static final double AXLE_PATH_CONVENTIONAL_MM = 3;

    /** Above this the chain model, which has no idler, cannot be trusted on these bikes. */
    private static final double AXLE_PATH_BEYOND_MODEL_MM = 10;

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

        InterpretationContext.LeverageShape leverageShape =
                leverageShape(leverage, curves.path("leverageCurve"));

        List<InterpretationContext.Evidence> evidence =
                evidenceFrom(descriptors, curves, capabilities, sagTravelMm(descriptors));
        InterpretationContext.Readings readings = readings(leverage, evidence, capabilities);

        // Generic rider-fit guidance is wanted; anything that pretends to know the rider's own
        // setup is not, and the curve alone cannot choose a spring (base-conocimiento section 3).
        List<String> allowedTopics = new ArrayList<>(List.of("leverage", "kickback", "riderFit"));
        if (antiSquat) allowedTopics.add("antiSquat");
        if (antiRise) allowedTopics.add("antiRise");
        if (worthMentioning(readings.axlePath())) allowedTopics.add("axlePath");

        List<String> forbiddenTopics = new ArrayList<>(List.of(
                "pressure", "clicks", "productModels", "brands", "guarantees",
                "springType", "volumeSpacers", "shockRecommendation"));
        if (!antiSquat) forbiddenTopics.add("antiSquat");
        if (!antiRise) forbiddenTopics.add("antiRise");
        if (!cogAwareKickback) forbiddenTopics.add("sprocketInfluence");
        if (!worthMentioning(readings.axlePath())) forbiddenTopics.add("axlePath");

        List<String> limits = new ArrayList<>(List.of(
                "Marked-photo geometry; not a laboratory measurement.",
                "The complete behaviour also depends on the shock, setup and rider."));
        if (referenceOnly) {
            limits.add("Reference wheel radii and centre of gravity; not user measurements.");
        }
        if ("BEYOND_MODEL".equals(readings.axlePath())) {
            limits.add("This much rearward travel belongs to a high pivot with an idler, "
                    + "which the direct-chain model does not represent.");
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
                readings,
                evidence,
                limits,
                allowedTopics,
                forbiddenTopics);
    }

    /**
     * The curve read in the three sections agreed with the project author: 0-40% initial
     * feel, 40-70% mid support, 70-100% reserve. The engine stores its own thirds, but the
     * initial feel does not end at sag, so the sections are recomputed from the raw curve.
     */
    private InterpretationContext.LeverageShape leverageShape(JsonNode leverage, JsonNode curve) {
        Double lrAt40 = ratioAtFraction(curve, INITIAL_FEEL_END);
        Double lrAt70 = ratioAtFraction(curve, MID_SUPPORT_END);
        Double lrStart = ratioAtFraction(curve, 0);
        Double lrEnd = ratioAtFraction(curve, 1);
        return new InterpretationContext.LeverageShape(
                progressionBand(number(leverage.path("totalProgressionPercent"))),
                trendBetween(lrStart, lrAt40),
                trendBetween(lrAt40, lrAt70),
                trendBetween(lrAt70, lrEnd),
                number(leverage.path("lrInitial")),
                number(leverage.path("lrAtSag")),
                lrAt40,
                lrAt70,
                number(leverage.path("lrFinal")));
    }

    /** A rising leverage ratio means the bike softens; a falling one means it firms up. */
    private String trendBetween(Double from, Double to) {
        if (from == null || to == null) {
            return null;
        }
        double change = to - from;
        if (change > FLAT_BAND) return "REGRESSIVE";
        if (change < -FLAT_BAND) return "PROGRESSIVE";
        return "LINEAR";
    }

    private Double ratioAtFraction(JsonNode curve, double fraction) {
        if (!curve.isArray() || curve.isEmpty()) {
            return null;
        }
        double totalTravelMm = 0;
        for (JsonNode sample : curve) {
            totalTravelMm = Math.max(totalTravelMm, sample.path("wheelTravelMm").asDouble(0));
        }
        return number(nearestTo(curve, totalTravelMm * fraction).path("ratio"));
    }

    /**
     * The band each figure falls into. These are the bands of the knowledge base, named and
     * nothing more: the provider writes the sentence, the backend only says where the number
     * lands.
     */
    private InterpretationContext.Readings readings(
            JsonNode leverage,
            List<InterpretationContext.Evidence> evidence,
            InterpretationContext.Capabilities capabilities) {
        return new InterpretationContext.Readings(
                progressionBand(number(leverage.path("totalProgressionPercent"))),
                capabilities.antiSquat() ? antiSquatBand(valueOf(evidence, "antiSquatAtSagPercent")) : null,
                capabilities.antiRise() ? antiRiseBand(valueOf(evidence, "antiRiseAtSagPercent")) : null,
                capabilities.cogAwareKickback() ? kickbackBand(valueOf(evidence, "maxKickbackDegrees")) : null,
                meanLeverageBand(number(leverage.path("lrMean"))),
                axlePathBand(valueOf(evidence, "maxRearwardMm")));
    }

    /**
     * The band of the progression over the whole travel. The engine classifies its band from
     * the useful progression instead, so classifying here keeps the band and the figure the
     * reader is shown saying the same thing. Thresholds: base-conocimiento section 3.
     */
    private String progressionBand(Double totalProgressionPercent) {
        if (totalProgressionPercent == null) return null;
        if (totalProgressionPercent < 0) return "REGRESSIVE";
        if (totalProgressionPercent < 5) return "LINEAR";
        if (totalProgressionPercent < 12) return "SLIGHTLY_PROGRESSIVE";
        if (totalProgressionPercent < 20) return "MEDIUM";
        if (totalProgressionPercent <= 30) return "HIGH";
        return "VERY_HIGH";
    }

    private String antiSquatBand(Double percent) {
        if (percent == null) return null;
        if (percent < 80) return "SOFT";
        if (percent < 100) return "BALANCED";
        if (percent < 120) return "FIRM";
        if (percent <= 140) return "VERY_FIRM";
        return "EXTREME";
    }

    /** Low lifts the rear under braking and copies the ground better; high settles it. */
    private String antiRiseBand(Double percent) {
        if (percent == null) return null;
        if (percent < 50) return "EXTENDS_UNDER_BRAKING";
        if (percent < 80) return "BALANCED";
        if (percent <= 110) return "SQUATS_UNDER_BRAKING";
        return "SITS_HARD_UNDER_BRAKING";
    }

    /** Measured in the climbing gear, which is the cog the wizard asks the rider for. */
    private String kickbackBand(Double degrees) {
        if (degrees == null) return null;
        if (degrees < 20) return "LOW";
        if (degrees < 35) return "MEDIUM";
        if (degrees <= 45) return "HIGH";
        return "VERY_HIGH";
    }

    private String meanLeverageBand(Double ratio) {
        if (ratio == null) return null;
        if (ratio <= 2.3) return "LOW";
        if (ratio < 2.9) return "TYPICAL";
        return "HIGH";
    }

    private String axlePathBand(Double rearwardMm) {
        if (rearwardMm == null) return null;
        if (rearwardMm < AXLE_PATH_CONVENTIONAL_MM) return "NOT_WORTH_MENTIONING";
        if (rearwardMm < 5) return "SLIGHT";
        if (rearwardMm <= AXLE_PATH_BEYOND_MODEL_MM) return "NOTICEABLE";
        return "BEYOND_MODEL";
    }

    private boolean worthMentioning(String axlePathBand) {
        return "SLIGHT".equals(axlePathBand) || "NOTICEABLE".equals(axlePathBand);
    }

    private Double valueOf(List<InterpretationContext.Evidence> evidence, String key) {
        return evidence.stream()
                .filter(item -> item.key().equals(key))
                .findFirst()
                .map(InterpretationContext.Evidence::value)
                .orElse(null);
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
        // Progression over the whole travel leads: it is the one percentage riders read.
        addNumber(evidence, "totalProgressionPercent", "%",
                leverage.path("totalProgressionPercent"));
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
        addNumber(evidence, "usefulProgressionPercent", "%",
                leverage.path("usefulProgressionPercent"));

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
        addNumber(evidence, key, unit, nearestTo(curve, sagTravelMm).path("percent"));
    }

    /** The sample whose wheel travel is closest to the target height of the curve. */
    private JsonNode nearestTo(JsonNode curve, double targetTravelMm) {
        JsonNode closest = curve.get(0);
        double smallestDistance = Double.MAX_VALUE;
        for (JsonNode sample : curve) {
            double distance = Math.abs(sample.path("wheelTravelMm").asDouble(Double.MAX_VALUE) - targetTravelMm);
            if (distance < smallestDistance) {
                smallestDistance = distance;
                closest = sample;
            }
        }
        return closest;
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

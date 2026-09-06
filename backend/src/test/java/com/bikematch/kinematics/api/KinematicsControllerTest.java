package com.bikematch.kinematics.api;

import com.bikematch.config.SecurityConfig;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import java.util.stream.Stream;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Acceptance test for POST /api/kinematics/preview: the frontend can request a full
 * kinematics calculation with no database involved. The valid body uses the Orange Surge
 * marked points (the engine's validated fixture); the invalid bodies exercise both error
 * paths handled by {@link ApiExceptionHandler} (@Valid rejection and a missing engine point).
 */
@WebMvcTest(KinematicsController.class)
@Import({KinematicsService.class, SecurityConfig.class})
class KinematicsControllerTest {

    @Autowired
    private MockMvc mockMvc;

    private static final String VALID_REQUEST = """
            {
              "points": [
                {"type": "MAIN_PIVOT",     "x": 208.3, "y": 167.8},
                {"type": "SHOCK_FRAME",    "x": 250.9, "y": 114.4},
                {"type": "SHOCK_SWINGARM", "x": 192.8, "y": 121.3},
                {"type": "BOTTOM_BRACKET", "x": 198.7, "y": 190.3},
                {"type": "REAR_AXLE",      "x": 86.0,  "y": 187.3},
                {"type": "FRONT_AXLE",     "x": 408.5, "y": 190.0}
              ],
              "eyeToEyeMm": 230.0,
              "parameters": {
                "shockStrokeMm": 65,
                "chainringTeeth": 34,
                "sprocketTeeth": 50,
                "declaredTravelMm": 164,
                "sagPercent": 30
              }
            }
            """;

    static Stream<String> invalidRequests() {
        return Stream.of(
                VALID_REQUEST.replace("192.8, \"y\": 121.3", "250.9, \"y\": 114.4"),
                VALID_REQUEST.replace("\"sagPercent\": 30", "\"sagPercent\": 100"),
                VALID_REQUEST.replace("\"sagPercent\": 30", "\"sagPercent\": 150"),
                VALID_REQUEST.replace("\"sagPercent\": 30", "\"sagPercent\": 0"),
                VALID_REQUEST.replace("\"chainringTeeth\": 34", "\"chainringTeeth\": 34.9"),
                VALID_REQUEST.replace("\"chainringTeeth\": 34", "\"chainringTeeth\": 0"),
                VALID_REQUEST.replace("\"shockStrokeMm\": 65", "\"shockStrokeMm\": -1"),
                VALID_REQUEST.replace("\"eyeToEyeMm\": 230.0", "\"eyeToEyeMm\": 1e200"),
                VALID_REQUEST.replace("\"x\": 208.3", "\"x\": 1e200"),
                VALID_REQUEST.replace("\"x\": 208.3", "\"x\": null"),
                VALID_REQUEST.replace("\"x\": 208.3,", ""),
                VALID_REQUEST.replace("\"x\": 208.3", "\"x\": \"NaN\""),
                VALID_REQUEST.replace("\"FRONT_AXLE\"", "\"MAIN_PIVOT\""),
                VALID_REQUEST.replace("{\"type\": \"FRONT_AXLE\",     \"x\": 408.5, \"y\": 190.0}", "null"),
                VALID_REQUEST.replace("\"FRONT_AXLE\"", "\"UNKNOWN\""),
                "{\"points\":"
        );
    }

    @ParameterizedTest
    @MethodSource("invalidRequests")
    void invalidInputReturnsAnExplained400InsteadOfNaNOrAnOpaque403(String body) throws Exception {
        mockMvc.perform(post("/api/kinematics/preview")
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").isNotEmpty());
    }

    @Test
    void validRequestReturnsTheFullPreview() throws Exception {
        mockMvc.perform(post("/api/kinematics/preview")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(VALID_REQUEST))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.leverageCurve").isNotEmpty())
                .andExpect(jsonPath("$.kickbackCurve").isNotEmpty())
                .andExpect(jsonPath("$.axlePath").isNotEmpty())
                .andExpect(jsonPath("$.leverageDescriptors.progressionBand").isNotEmpty())
                .andExpect(jsonPath("$.axlePathDescriptors.maxRearwardMm").isNumber())
                .andExpect(jsonPath("$.travelCheck.calculatedTravelMm").isNumber())
                .andExpect(jsonPath("$.conditions.chainringTeeth").value(34));
    }

    @Test
    void emptyPointsIsRejectedWith400() throws Exception {
        String body = """
                {
                  "points": [],
                  "eyeToEyeMm": 230.0,
                  "parameters": {
                    "shockStrokeMm": 65, "chainringTeeth": 34, "sprocketTeeth": 50,
                    "declaredTravelMm": 164, "sagPercent": 30
                  }
                }
                """;

        mockMvc.perform(post("/api/kinematics/preview")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest());
    }

    @Test
    void missingRequiredPointIsRejectedWith400() throws Exception {
        // Well-formed and passes @Valid, but MAIN_PIVOT is absent -> the engine fails fast.
        String body = """
                {
                  "points": [
                    {"type": "SHOCK_FRAME",    "x": 250.9, "y": 114.4},
                    {"type": "SHOCK_SWINGARM", "x": 192.8, "y": 121.3},
                    {"type": "BOTTOM_BRACKET", "x": 198.7, "y": 190.3},
                    {"type": "REAR_AXLE",      "x": 86.0,  "y": 187.3},
                    {"type": "FRONT_AXLE",     "x": 408.5, "y": 190.0}
                  ],
                  "eyeToEyeMm": 230.0,
                  "parameters": {
                    "shockStrokeMm": 65, "chainringTeeth": 34, "sprocketTeeth": 50,
                    "declaredTravelMm": 164, "sagPercent": 30
                  }
                }
                """;

        mockMvc.perform(post("/api/kinematics/preview")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest());
    }
}

package com.bikematch.interpretation.api;

import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.bikematch.api.ApiExceptionHandler;
import com.bikematch.auth.JwtAuthenticationFilter;
import com.bikematch.auth.JwtService;
import com.bikematch.config.RestAccessDeniedHandler;
import com.bikematch.config.RestAuthenticationEntryPoint;
import com.bikematch.config.SecurityConfig;
import com.bikematch.interpretation.InterpretationService;
import com.bikematch.interpretation.InterpretationService.InterpretationView;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import io.jsonwebtoken.Claims;
import java.time.Instant;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(InterpretationController.class)
@Import({
        ApiExceptionHandler.class,
        SecurityConfig.class,
        JwtAuthenticationFilter.class,
        RestAuthenticationEntryPoint.class,
        RestAccessDeniedHandler.class
})
class InterpretationControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private InterpretationService interpretationService;

    @MockitoBean
    private JwtService jwtService;

    @Test
    void publicBikeCanReadCachedInterpretation() throws Exception {
        given(interpretationService.get(7L, null, "en")).willReturn(view());

        mockMvc.perform(get("/api/bikes/{bikeId}/interpretation", 7L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.source").value("RULES"))
                .andExpect(jsonPath("$.summary").value("A concise explanation."))
                .andExpect(jsonPath("$.evidence[0].key").value("usefulProgressionPercent"))
                .andExpect(jsonPath("$.fallback").value(true));
    }

    @Test
    void onlyAnAuthenticatedOwnerCanGenerateInterpretation() throws Exception {
        Claims claims = mock(Claims.class);
        given(claims.getSubject()).willReturn("42");
        given(claims.get("role", String.class)).willReturn("USER");
        given(jwtService.parseToken("user-token")).willReturn(claims);
        given(interpretationService.generate(7L, 42L, "en")).willReturn(view());

        mockMvc.perform(post("/api/bikes/{bikeId}/interpretation", 7L)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer user-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.providerVersion").value("rules-1"));

        mockMvc.perform(post("/api/bikes/{bikeId}/interpretation", 7L))
                .andExpect(status().isUnauthorized());
    }

    private InterpretationView view() {
        var evidence = JsonNodeFactory.instance.arrayNode();
        evidence.addObject()
                .put("key", "usefulProgressionPercent")
                .put("value", 18)
                .put("unit", "%");
        return new InterpretationView(
                1,
                1,
                "kinematics-rules-1",
                "en",
                "RULES",
                "rules-1",
                "A concise explanation.",
                evidence,
                Instant.parse("2026-09-14T12:00:00Z"),
                true);
    }
}

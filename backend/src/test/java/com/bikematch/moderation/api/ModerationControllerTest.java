package com.bikematch.moderation.api;

import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.bikematch.auth.JwtAuthenticationFilter;
import com.bikematch.auth.JwtService;
import com.bikematch.bike.BikeCategory;
import com.bikematch.config.RestAccessDeniedHandler;
import com.bikematch.config.RestAuthenticationEntryPoint;
import com.bikematch.config.SecurityConfig;
import com.bikematch.moderation.ListPendingBikesService;
import com.bikematch.moderation.PendingBikeSummary;
import io.jsonwebtoken.Claims;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(ModerationController.class)
@Import({
        SecurityConfig.class,
        JwtAuthenticationFilter.class,
        RestAuthenticationEntryPoint.class,
        RestAccessDeniedHandler.class
})
class ModerationControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ListPendingBikesService listPendingBikesService;

    @MockitoBean
    private JwtService jwtService;

    @Test
    void moderatorCanListThePendingBikes() throws Exception {
        authenticate("moderator-token", "7", "MODERATOR");
        given(listPendingBikesService.list()).willReturn(List.of(new PendingBikeSummary(
                12L, "Orange", "Stage 6", (short) 2020, BikeCategory.ENDURO,
                "https://example.com/stage-6.jpg", "david", Instant.parse("2026-09-14T12:00:00Z"))));

        mockMvc.perform(get("/api/moderation/pending")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer moderator-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(12))
                .andExpect(jsonPath("$[0].ownerUsername").value("david"))
                .andExpect(jsonPath("$[0].requestedAt").value("2026-09-14T12:00:00Z"));

        verify(listPendingBikesService).list();
    }

    @Test
    void userCannotListThePendingBikes() throws Exception {
        authenticate("user-token", "42", "USER");

        mockMvc.perform(get("/api/moderation/pending")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer user-token"))
                .andExpect(status().isForbidden());

        verify(listPendingBikesService, never()).list();
    }

    @Test
    void anonymousVisitorCannotListThePendingBikes() throws Exception {
        mockMvc.perform(get("/api/moderation/pending"))
                .andExpect(status().isUnauthorized());

        verify(listPendingBikesService, never()).list();
    }

    private void authenticate(String token, String userId, String role) {
        Claims claims = mock(Claims.class);
        given(claims.getSubject()).willReturn(userId);
        given(claims.get("role", String.class)).willReturn(role);
        given(jwtService.parseToken(token)).willReturn(claims);
    }
}

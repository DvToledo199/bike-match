package com.bikematch.bike.api;

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
import com.bikematch.bike.BikeStatus;
import com.bikematch.bike.ListOwnedBikesService;
import com.bikematch.bike.OwnedBikeSummary;
import com.bikematch.config.RestAccessDeniedHandler;
import com.bikematch.config.RestAuthenticationEntryPoint;
import com.bikematch.config.SecurityConfig;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.MalformedJwtException;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(MyBikesController.class)
@Import({
        SecurityConfig.class,
        JwtAuthenticationFilter.class,
        RestAuthenticationEntryPoint.class,
        RestAccessDeniedHandler.class
})
class MyBikesControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ListOwnedBikesService listOwnedBikesService;

    @MockitoBean
    private JwtService jwtService;

    @Test
    void authenticatedUserReceivesOnlySummaryFieldsForTheirBikes() throws Exception {
        authenticateUserToken();
        given(listOwnedBikesService.list(42L)).willReturn(List.of(summary()));

        mockMvc.perform(get("/api/my-bikes")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer user-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(7))
                .andExpect(jsonPath("$[0].brand").value("Orange"))
                .andExpect(jsonPath("$[0].model").value("Stage 6"))
                .andExpect(jsonPath("$[0].status").value("PRIVATE"))
                .andExpect(jsonPath("$[0].analyzed").value(true))
                .andExpect(jsonPath("$[0].linkagePoints").doesNotExist())
                .andExpect(jsonPath("$[0].curves").doesNotExist());

        verify(listOwnedBikesService).list(42L);
    }

    @Test
    void authenticatedUserWithNoBikesReceivesAnEmptyList() throws Exception {
        authenticateUserToken();
        given(listOwnedBikesService.list(42L)).willReturn(List.of());

        mockMvc.perform(get("/api/my-bikes")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer user-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isEmpty());
    }

    @Test
    void missingTokenCannotListBikes() throws Exception {
        mockMvc.perform(get("/api/my-bikes"))
                .andExpect(status().isUnauthorized());

        verify(listOwnedBikesService, never()).list(42L);
    }

    @Test
    void invalidTokenCannotListBikes() throws Exception {
        given(jwtService.parseToken("invalid-token"))
                .willThrow(new MalformedJwtException("Invalid token"));

        mockMvc.perform(get("/api/my-bikes")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer invalid-token"))
                .andExpect(status().isUnauthorized());

        verify(listOwnedBikesService, never()).list(42L);
    }

    private void authenticateUserToken() {
        Claims claims = mock(Claims.class);
        given(claims.getSubject()).willReturn("42");
        given(claims.get("role", String.class)).willReturn("USER");
        given(jwtService.parseToken("user-token")).willReturn(claims);
    }

    private OwnedBikeSummary summary() {
        return new OwnedBikeSummary(
                7L, "Orange", "Stage 6", (short) 2020, BikeCategory.ENDURO,
                "https://example.com/bike.jpg", BikeStatus.PRIVATE, true,
                Instant.parse("2026-09-14T12:00:00Z"));
    }
}

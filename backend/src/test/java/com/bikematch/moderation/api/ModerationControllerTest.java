package com.bikematch.moderation.api;

import static org.hamcrest.Matchers.startsWith;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.bikematch.api.ApiExceptionHandler;
import com.bikematch.auth.JwtAuthenticationFilter;
import com.bikematch.auth.JwtService;
import com.bikematch.bike.Bike;
import com.bikematch.bike.BikeCategory;
import com.bikematch.bike.BikeNotFoundException;
import com.bikematch.bike.BikeNotPendingException;
import com.bikematch.bike.BikeStatus;
import com.bikematch.config.RestAccessDeniedHandler;
import com.bikematch.config.RestAuthenticationEntryPoint;
import com.bikematch.config.SecurityConfig;
import com.bikematch.moderation.ListPendingBikesService;
import com.bikematch.moderation.ModerateBikePublicationService;
import com.bikematch.moderation.PendingBikeSummary;
import com.bikematch.moderation.RemoveBikeService;
import io.jsonwebtoken.Claims;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(ModerationController.class)
@Import({
        ApiExceptionHandler.class,
        SecurityConfig.class,
        JwtAuthenticationFilter.class,
        RestAuthenticationEntryPoint.class,
        RestAccessDeniedHandler.class
})
class ModerationControllerTest {

    private static final String REMOVAL_REQUEST = """
            {"reason": "Photo taken from another website"}
            """;

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ListPendingBikesService listPendingBikesService;

    @MockitoBean
    private ModerateBikePublicationService moderateBikePublicationService;

    @MockitoBean
    private RemoveBikeService removeBikeService;

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

    @Test
    void moderatorCanApproveAPendingBike() throws Exception {
        authenticate("moderator-token", "7", "MODERATOR");
        Bike bike = mock(Bike.class);
        given(bike.getId()).willReturn(12L);
        given(bike.getStatus()).willReturn(BikeStatus.PUBLIC);
        given(moderateBikePublicationService.approve(12L)).willReturn(bike);

        mockMvc.perform(post("/api/moderation/{bikeId}/approve", 12L)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer moderator-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(12))
                .andExpect(jsonPath("$.status").value("PUBLIC"));

        verify(moderateBikePublicationService).approve(12L);
    }

    @Test
    void moderatorCanRejectAPendingBike() throws Exception {
        authenticate("moderator-token", "7", "MODERATOR");
        Bike bike = mock(Bike.class);
        given(bike.getId()).willReturn(12L);
        given(bike.getStatus()).willReturn(BikeStatus.REJECTED);
        given(moderateBikePublicationService.reject(12L)).willReturn(bike);

        mockMvc.perform(post("/api/moderation/{bikeId}/reject", 12L)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer moderator-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("REJECTED"));

        verify(moderateBikePublicationService).reject(12L);
    }

    @Test
    void userCannotApproveOrRejectAPendingBike() throws Exception {
        authenticate("user-token", "42", "USER");

        mockMvc.perform(post("/api/moderation/{bikeId}/approve", 12L)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer user-token"))
                .andExpect(status().isForbidden());
        mockMvc.perform(post("/api/moderation/{bikeId}/reject", 12L)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer user-token"))
                .andExpect(status().isForbidden());

        verify(moderateBikePublicationService, never()).approve(12L);
        verify(moderateBikePublicationService, never()).reject(12L);
    }

    @Test
    void missingBikeReturnsNotFoundWhenModerating() throws Exception {
        authenticate("moderator-token", "7", "MODERATOR");
        given(moderateBikePublicationService.approve(12L))
                .willThrow(new BikeNotFoundException());

        mockMvc.perform(post("/api/moderation/{bikeId}/approve", 12L)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer moderator-token"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.detail").value("Bike not found"));
    }

    @Test
    void nonPendingBikeReturnsConflictWhenModerating() throws Exception {
        authenticate("moderator-token", "7", "MODERATOR");
        given(moderateBikePublicationService.reject(12L))
                .willThrow(new BikeNotPendingException());

        mockMvc.perform(post("/api/moderation/{bikeId}/reject", 12L)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer moderator-token"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail")
                        .value("Only pending bikes can be approved or rejected"));
    }

    @Test
    void moderatorRemovesABikeWithAReason() throws Exception {
        authenticate("moderator-token", "7", "MODERATOR");

        mockMvc.perform(post("/api/moderation/{bikeId}/remove", 12L)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer moderator-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(REMOVAL_REQUEST))
                .andExpect(status().isNoContent());

        verify(removeBikeService).remove(12L, "Photo taken from another website");
    }

    @Test
    void removingABikeRequiresAReason() throws Exception {
        authenticate("moderator-token", "7", "MODERATOR");

        mockMvc.perform(post("/api/moderation/{bikeId}/remove", 12L)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer moderator-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"reason\": \"   \"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value(startsWith("reason ")));

        verify(removeBikeService, never()).remove(anyLong(), anyString());
    }

    @Test
    void userCannotRemoveABike() throws Exception {
        authenticate("user-token", "42", "USER");

        mockMvc.perform(post("/api/moderation/{bikeId}/remove", 12L)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer user-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(REMOVAL_REQUEST))
                .andExpect(status().isForbidden());

        verify(removeBikeService, never()).remove(anyLong(), anyString());
    }

    @Test
    void privateOrMissingBikeReturnsNotFoundWhenRemoving() throws Exception {
        authenticate("moderator-token", "7", "MODERATOR");
        willThrow(new BikeNotFoundException())
                .given(removeBikeService).remove(12L, "Photo taken from another website");

        mockMvc.perform(post("/api/moderation/{bikeId}/remove", 12L)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer moderator-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(REMOVAL_REQUEST))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.detail").value("Bike not found"));
    }

    private void authenticate(String token, String userId, String role) {
        Claims claims = mock(Claims.class);
        given(claims.getSubject()).willReturn(userId);
        given(claims.get("role", String.class)).willReturn(role);
        given(jwtService.parseToken(token)).willReturn(claims);
    }
}

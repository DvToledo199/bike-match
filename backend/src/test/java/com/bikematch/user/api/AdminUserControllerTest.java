package com.bikematch.user.api;

import static org.hamcrest.Matchers.aMapWithSize;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.bikematch.api.ApiExceptionHandler;
import com.bikematch.auth.JwtAuthenticationFilter;
import com.bikematch.auth.JwtService;
import com.bikematch.config.RestAccessDeniedHandler;
import com.bikematch.config.RestAuthenticationEntryPoint;
import com.bikematch.config.SecurityConfig;
import com.bikematch.user.CannotChangeOwnRoleException;
import com.bikematch.user.ChangeUserRoleService;
import com.bikematch.user.ListUsersService;
import com.bikematch.user.Role;
import com.bikematch.user.UserNotFoundException;
import com.bikematch.user.UserSummary;
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

@WebMvcTest(AdminUserController.class)
@Import({
        ApiExceptionHandler.class,
        SecurityConfig.class,
        JwtAuthenticationFilter.class,
        RestAuthenticationEntryPoint.class,
        RestAccessDeniedHandler.class
})
class AdminUserControllerTest {

    private static final String MODERATOR_BODY = """
            {"role": "MODERATOR"}
            """;

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ListUsersService listUsersService;

    @MockitoBean
    private ChangeUserRoleService changeUserRoleService;

    @MockitoBean
    private JwtService jwtService;

    @Test
    void administratorSeesAccountEmailsButNotPasswordHashes() throws Exception {
        authenticate("admin-token", "90", "ADMIN");
        given(listUsersService.list()).willReturn(List.of(
                new UserSummary(88L, "rider", "rider@example.com", Role.USER,
                        Instant.parse("2026-09-15T16:58:20Z"))));

        mockMvc.perform(get("/api/admin/users")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer admin-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0]", aMapWithSize(5)))
                .andExpect(jsonPath("$[0].id").value(88))
                .andExpect(jsonPath("$[0].username").value("rider"))
                .andExpect(jsonPath("$[0].email").value("rider@example.com"))
                .andExpect(jsonPath("$[0].role").value("USER"))
                .andExpect(jsonPath("$[0].createdAt").value("2026-09-15T16:58:20Z"))
                .andExpect(jsonPath("$[0].passwordHash").doesNotExist());
    }

    @Test
    void anOrdinaryUserIsRefused() throws Exception {
        authenticate("user-token", "88", "USER");

        mockMvc.perform(get("/api/admin/users")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer user-token"))
                .andExpect(status().isForbidden())
                .andExpect(content().string(""));

        verify(listUsersService, never()).list();
    }

    @Test
    void aModeratorCannotReadAccountEmails() throws Exception {
        authenticate("moderator-token", "89", "MODERATOR");

        mockMvc.perform(get("/api/admin/users")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer moderator-token"))
                .andExpect(status().isForbidden())
                .andExpect(content().string(""));

        verify(listUsersService, never()).list();
    }

    /** Managing accounts is not a moderator's job: the two roles are not interchangeable. */
    @Test
    void aModeratorIsRefusedToo() throws Exception {
        authenticate("moderator-token", "89", "MODERATOR");

        mockMvc.perform(put("/api/admin/users/{userId}/role", 88L)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer moderator-token")
                        .contentType(MediaType.APPLICATION_JSON).content(MODERATOR_BODY))
                .andExpect(status().isForbidden());

        verify(changeUserRoleService, never()).changeRole(anyLong(), any(), anyLong());
    }

    @Test
    void anonymousVisitorIsRejected() throws Exception {
        mockMvc.perform(get("/api/admin/users"))
                .andExpect(status().isUnauthorized())
                .andExpect(content().string(""));

        verify(listUsersService, never()).list();
    }

    @Test
    void administratorGrantsTheModeratorRole() throws Exception {
        authenticate("admin-token", "90", "ADMIN");

        mockMvc.perform(put("/api/admin/users/{userId}/role", 88L)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer admin-token")
                        .contentType(MediaType.APPLICATION_JSON).content(MODERATOR_BODY))
                .andExpect(status().isNoContent());

        // The caller identity travels from the token to the service, which needs it for its rule.
        verify(changeUserRoleService).changeRole(88L, Role.MODERATOR, 90L);
    }

    @Test
    void changingYourOwnRoleAnswersConflict() throws Exception {
        authenticate("admin-token", "90", "ADMIN");
        willThrow(new CannotChangeOwnRoleException())
                .given(changeUserRoleService).changeRole(90L, Role.MODERATOR, 90L);

        mockMvc.perform(put("/api/admin/users/{userId}/role", 90L)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer admin-token")
                        .contentType(MediaType.APPLICATION_JSON).content(MODERATOR_BODY))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail").value("An administrator cannot change their own role"));
    }

    @Test
    void anUnknownAccountAnswersNotFound() throws Exception {
        authenticate("admin-token", "90", "ADMIN");
        willThrow(new UserNotFoundException())
                .given(changeUserRoleService).changeRole(404L, Role.MODERATOR, 90L);

        mockMvc.perform(put("/api/admin/users/{userId}/role", 404L)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer admin-token")
                        .contentType(MediaType.APPLICATION_JSON).content(MODERATOR_BODY))
                .andExpect(status().isNotFound());
    }

    /** A role outside the enum never reaches the service: it fails while reading the JSON. */
    @Test
    void anInvalidRoleIsRejected() throws Exception {
        authenticate("admin-token", "90", "ADMIN");

        mockMvc.perform(put("/api/admin/users/{userId}/role", 88L)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer admin-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"role\": \"SUPERBOSS\"}"))
                .andExpect(status().isBadRequest());

        verify(changeUserRoleService, never()).changeRole(anyLong(), any(), anyLong());
    }

    private void authenticate(String token, String userId, String role) {
        Claims claims = mock(Claims.class);
        given(claims.getSubject()).willReturn(userId);
        given(claims.get("role", String.class)).willReturn(role);
        given(jwtService.parseToken(token)).willReturn(claims);
    }
}

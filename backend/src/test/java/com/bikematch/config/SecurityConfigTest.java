package com.bikematch.config;

import com.bikematch.auth.JwtAuthenticationFilter;
import com.bikematch.auth.JwtService;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.MalformedJwtException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = SecurityConfigTest.SecurityProbeController.class)
@Import({
        SecurityConfigTest.SecurityProbeController.class,
        SecurityConfig.class,
        JwtAuthenticationFilter.class,
        RestAuthenticationEntryPoint.class,
        RestAccessDeniedHandler.class
})
class SecurityConfigTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private JwtService jwtService;

    @Test
    void publicHealthDoesNotNeedAToken() throws Exception {
        mockMvc.perform(get("/api/health"))
                .andExpect(status().isOk());
    }

    @Test
    void protectedRouteWithoutTokenReturns401() throws Exception {
        mockMvc.perform(get("/api/bikes/test"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void invalidTokenReturns401() throws Exception {
        given(jwtService.parseToken("invalid-token"))
                .willThrow(new MalformedJwtException("Invalid token"));

        mockMvc.perform(get("/api/bikes/test")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer invalid-token"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void userTokenCanAccessAnAuthenticatedRoute() throws Exception {
        Claims userClaims = claims("42", "USER");
        given(jwtService.parseToken("user-token")).willReturn(userClaims);

        mockMvc.perform(get("/api/bikes/test")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer user-token"))
                .andExpect(status().isOk());
    }

    @Test
    void userTokenCannotAccessModeratorRoute() throws Exception {
        Claims userClaims = claims("42", "USER");
        given(jwtService.parseToken("user-token")).willReturn(userClaims);

        mockMvc.perform(get("/api/moderation/test")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer user-token"))
                .andExpect(status().isForbidden());
    }

    @Test
    void moderatorTokenCanAccessModeratorRoute() throws Exception {
        Claims moderatorClaims = claims("7", "MODERATOR");
        given(jwtService.parseToken("moderator-token")).willReturn(moderatorClaims);

        mockMvc.perform(get("/api/moderation/test")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer moderator-token"))
                .andExpect(status().isOk());
    }

    private Claims claims(String userId, String role) {
        Claims claims = mock(Claims.class);
        given(claims.getSubject()).willReturn(userId);
        given(claims.get("role", String.class)).willReturn(role);
        return claims;
    }

    @RestController
    public static class SecurityProbeController {
        @GetMapping("/api/health")
        String health() {
            return "UP";
        }

        @GetMapping("/api/bikes/test")
        String authenticatedRoute() {
            return "available to authenticated users";
        }

        @GetMapping("/api/moderation/test")
        String moderatorRoute() {
            return "available to moderators";
        }
    }
}

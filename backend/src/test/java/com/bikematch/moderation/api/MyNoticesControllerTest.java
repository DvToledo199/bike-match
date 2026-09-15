package com.bikematch.moderation.api;

import static org.mockito.ArgumentMatchers.anyLong;
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
import com.bikematch.config.RestAccessDeniedHandler;
import com.bikematch.config.RestAuthenticationEntryPoint;
import com.bikematch.config.SecurityConfig;
import com.bikematch.moderation.BikeRemovalNotice;
import com.bikematch.moderation.RemovalNoticeNotFoundException;
import com.bikematch.moderation.RemovalNoticeService;
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

@WebMvcTest(MyNoticesController.class)
@Import({
        ApiExceptionHandler.class,
        SecurityConfig.class,
        JwtAuthenticationFilter.class,
        RestAuthenticationEntryPoint.class,
        RestAccessDeniedHandler.class
})
class MyNoticesControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private RemovalNoticeService removalNoticeService;

    @MockitoBean
    private JwtService jwtService;

    @Test
    void ownerReadsTheNoticesAboutTheirRemovedBikes() throws Exception {
        authenticateUserToken();
        BikeRemovalNotice notice = mock(BikeRemovalNotice.class);
        given(notice.getId()).willReturn(3L);
        given(notice.getBrand()).willReturn("Orange");
        given(notice.getModel()).willReturn("Stage 6");
        given(notice.getReason()).willReturn("Photo taken from another website");
        given(notice.getRemovedAt()).willReturn(Instant.parse("2026-09-15T18:00:00Z"));
        given(removalNoticeService.listPending(42L)).willReturn(List.of(notice));

        mockMvc.perform(get("/api/my-notices")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer user-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(3))
                .andExpect(jsonPath("$[0].brand").value("Orange"))
                .andExpect(jsonPath("$[0].model").value("Stage 6"))
                .andExpect(jsonPath("$[0].reason").value("Photo taken from another website"))
                .andExpect(jsonPath("$[0].removedAt").value("2026-09-15T18:00:00Z"));
    }

    @Test
    void missingTokenCannotReadNotices() throws Exception {
        mockMvc.perform(get("/api/my-notices"))
                .andExpect(status().isUnauthorized());

        verify(removalNoticeService, never()).listPending(anyLong());
    }

    @Test
    void ownerDismissesANoticeWith204() throws Exception {
        authenticateUserToken();

        mockMvc.perform(post("/api/my-notices/{noticeId}/dismiss", 3L)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer user-token"))
                .andExpect(status().isNoContent());

        verify(removalNoticeService).dismiss(42L, 3L);
    }

    @Test
    void missingOrForeignNoticeReturns404() throws Exception {
        authenticateUserToken();
        willThrow(new RemovalNoticeNotFoundException())
                .given(removalNoticeService).dismiss(42L, 3L);

        mockMvc.perform(post("/api/my-notices/{noticeId}/dismiss", 3L)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer user-token"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.detail").value("Notice not found"));
    }

    private void authenticateUserToken() {
        Claims claims = mock(Claims.class);
        given(claims.getSubject()).willReturn("42");
        given(claims.get("role", String.class)).willReturn("USER");
        given(jwtService.parseToken("user-token")).willReturn(claims);
    }
}

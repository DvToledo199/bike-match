package com.bikematch.bike.api;

import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.bikematch.api.ApiExceptionHandler;
import com.bikematch.auth.JwtAuthenticationFilter;
import com.bikematch.auth.JwtService;
import com.bikematch.bike.BikeCategory;
import com.bikematch.bike.ListPublicBikesService;
import com.bikematch.bike.PublicBikeSummary;
import com.bikematch.config.RestAccessDeniedHandler;
import com.bikematch.config.RestAuthenticationEntryPoint;
import com.bikematch.config.SecurityConfig;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

@WebMvcTest(BikeCatalogController.class)
@Import({
        ApiExceptionHandler.class,
        SecurityConfig.class,
        JwtAuthenticationFilter.class,
        RestAuthenticationEntryPoint.class,
        RestAccessDeniedHandler.class
})
class BikeCatalogControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ListPublicBikesService listPublicBikesService;

    @MockitoBean
    private JwtService jwtService;

    @Test
    void publicCatalogReturnsOnlyTheRequestedPageWithoutToken() throws Exception {
        given(listPublicBikesService.list(null, 0)).willReturn(new PageImpl<>(
                List.of(summary()), PageRequest.of(0, ListPublicBikesService.PAGE_SIZE), 13));

        mockMvc.perform(get("/api/bikes"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[0].id").value(7))
                .andExpect(jsonPath("$.items[0].brand").value("Orange"))
                .andExpect(jsonPath("$.items[0].category").value("ENDURO"))
                .andExpect(jsonPath("$.page").value(0))
                .andExpect(jsonPath("$.pageSize").value(12))
                .andExpect(jsonPath("$.totalElements").value(13))
                .andExpect(jsonPath("$.totalPages").value(2))
                .andExpect(jsonPath("$.hasNext").value(true));

        verify(listPublicBikesService).list(null, 0);
    }

    @Test
    void acceptsCategoryAndPageFilters() throws Exception {
        given(listPublicBikesService.list(BikeCategory.E_ENDURO, 2)).willReturn(
                new PageImpl<>(List.of(), PageRequest.of(2, ListPublicBikesService.PAGE_SIZE), 0));

        mockMvc.perform(get("/api/bikes")
                        .param("category", "e_enduro")
                        .param("page", "2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items").isEmpty())
                .andExpect(jsonPath("$.page").value(2))
                .andExpect(jsonPath("$.hasNext").value(false));

        verify(listPublicBikesService).list(BikeCategory.E_ENDURO, 2);
    }

    @Test
    void invalidCategoryReturns400() throws Exception {
        mockMvc.perform(get("/api/bikes").param("category", "TRIAL"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail")
                        .value("Category must be ENDURO, E_ENDURO or DOWNHILL"));

        verify(listPublicBikesService, never()).list(null, 0);
    }

    @Test
    void negativePageReturns400() throws Exception {
        mockMvc.perform(get("/api/bikes").param("page", "-1"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value("Page must be zero or greater"));

        verify(listPublicBikesService, never()).list(null, -1);
    }

    private PublicBikeSummary summary() {
        return new PublicBikeSummary(
                7L, "Orange", "Stage 6", (short) 2020, BikeCategory.ENDURO,
                "https://example.com/stage.jpg", Instant.parse("2026-09-14T12:00:00Z"));
    }
}

package com.bikematch.bike.api;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.bikematch.api.ApiExceptionHandler;
import com.bikematch.auth.JwtAuthenticationFilter;
import com.bikematch.auth.JwtService;
import com.bikematch.bike.Bike;
import com.bikematch.bike.BikeDetails;
import com.bikematch.bike.BikeStatus;
import com.bikematch.bike.CreateBikeService;
import com.bikematch.config.RestAccessDeniedHandler;
import com.bikematch.config.RestAuthenticationEntryPoint;
import com.bikematch.config.SecurityConfig;
import io.jsonwebtoken.Claims;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(BikeController.class)
@Import({
        ApiExceptionHandler.class,
        SecurityConfig.class,
        JwtAuthenticationFilter.class,
        RestAuthenticationEntryPoint.class,
        RestAccessDeniedHandler.class
})
class BikeControllerTest {

    private static final String VALID_REQUEST = """
            {
              "brand": "Orange",
              "model": "Stage 6",
              "modelYear": 2020,
              "category": "ENDURO",
              "suspensionLayout": "SINGLE_PIVOT",
              "declaredTravelMm": 150,
              "shockEyeToEyeMm": 230,
              "shockStrokeMm": 65,
              "wheelConfiguration": "FULL_29",
              "cassetteType": "TWELVE_SPEED",
              "chainringTeeth": 32,
              "sprocketTeeth": 50,
              "sagPercent": 30,
              "ownerId": 999,
              "status": "PUBLIC"
            }
            """;

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private CreateBikeService createBikeService;

    @MockitoBean
    private JwtService jwtService;

    @Test
    void authenticatedUserCreatesAPrivateBikeWith201() throws Exception {
        authenticateUserToken();
        Bike savedBike = mock(Bike.class);
        given(savedBike.getId()).willReturn(7L);
        given(savedBike.getStatus()).willReturn(BikeStatus.PRIVATE);
        given(createBikeService.create(eq(42L), any(BikeDetails.class)))
                .willReturn(savedBike);

        mockMvc.perform(post("/api/bikes")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer user-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(VALID_REQUEST))
                .andExpect(status().isCreated())
                .andExpect(header().string(HttpHeaders.LOCATION, "/api/bikes/7"))
                .andExpect(jsonPath("$.id").value(7))
                .andExpect(jsonPath("$.status").value("PRIVATE"))
                .andExpect(jsonPath("$.ownerId").doesNotExist());

        ArgumentCaptor<BikeDetails> detailsCaptor = ArgumentCaptor.forClass(BikeDetails.class);
        verify(createBikeService).create(eq(42L), detailsCaptor.capture());
        org.assertj.core.api.Assertions.assertThat(detailsCaptor.getValue().brand())
                .isEqualTo("Orange");
    }

    @Test
    void missingTokenReturns401AndDoesNotCreateABike() throws Exception {
        mockMvc.perform(post("/api/bikes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(VALID_REQUEST))
                .andExpect(status().isUnauthorized());

        verify(createBikeService, never()).create(anyLong(), any(BikeDetails.class));
    }

    @Test
    void modelYearMayBeOmitted() throws Exception {
        authenticateUserToken();
        Bike savedBike = mock(Bike.class);
        given(savedBike.getId()).willReturn(7L);
        given(savedBike.getStatus()).willReturn(BikeStatus.PRIVATE);
        given(createBikeService.create(eq(42L), any(BikeDetails.class)))
                .willReturn(savedBike);
        String requestWithoutModelYear = VALID_REQUEST.replace(
                "  \"modelYear\": 2020,\n", "");

        mockMvc.perform(post("/api/bikes")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer user-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestWithoutModelYear))
                .andExpect(status().isCreated());

        ArgumentCaptor<BikeDetails> detailsCaptor = ArgumentCaptor.forClass(BikeDetails.class);
        verify(createBikeService).create(eq(42L), detailsCaptor.capture());
        org.assertj.core.api.Assertions.assertThat(detailsCaptor.getValue().modelYear()).isNull();
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "\"brand\": \"Orange\"",
            "\"modelYear\": 2020",
            "\"declaredTravelMm\": 150",
            "\"chainringTeeth\": 32"
    })
    void invalidMetadataReturns400(String validField) throws Exception {
        authenticateUserToken();
        String invalidValue = switch (validField) {
            case "\"brand\": \"Orange\"" -> "\"brand\": \"\"";
            case "\"modelYear\": 2020" -> "\"modelYear\": 1800";
            case "\"declaredTravelMm\": 150" -> "\"declaredTravelMm\": 49";
            default -> "\"chainringTeeth\": 19";
        };

        mockMvc.perform(post("/api/bikes")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer user-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(VALID_REQUEST.replace(validField, invalidValue)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").isNotEmpty());

        verify(createBikeService, never()).create(anyLong(), any(BikeDetails.class));
    }

    @Test
    void unknownEnumReturns400() throws Exception {
        authenticateUserToken();

        mockMvc.perform(post("/api/bikes")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer user-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(VALID_REQUEST.replace("ENDURO", "TRICYCLE")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value("Send valid JSON"));

        verify(createBikeService, never()).create(anyLong(), any(BikeDetails.class));
    }

    private void authenticateUserToken() {
        Claims userClaims = claims("42", "USER");
        given(jwtService.parseToken("user-token")).willReturn(userClaims);
    }

    private Claims claims(String userId, String role) {
        Claims claims = mock(Claims.class);
        given(claims.getSubject()).willReturn(userId);
        given(claims.get("role", String.class)).willReturn(role);
        return claims;
    }
}

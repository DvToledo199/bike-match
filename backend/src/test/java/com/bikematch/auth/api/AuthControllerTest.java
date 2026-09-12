package com.bikematch.auth.api;

import com.bikematch.api.ApiExceptionHandler;
import com.bikematch.auth.AccountAlreadyExistsException;
import com.bikematch.auth.InvalidCredentialsException;
import com.bikematch.auth.LoginService;
import com.bikematch.auth.RegistrationService;
import com.bikematch.auth.JwtAuthenticationFilter;
import com.bikematch.auth.JwtService;
import com.bikematch.config.SecurityConfig;
import com.bikematch.config.RestAccessDeniedHandler;
import com.bikematch.config.RestAuthenticationEntryPoint;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.stream.Stream;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AuthController.class)
@Import({
        ApiExceptionHandler.class,
        SecurityConfig.class,
        JwtAuthenticationFilter.class,
        RestAuthenticationEntryPoint.class,
        RestAccessDeniedHandler.class
})
class AuthControllerTest {

    private static final String VALID_REQUEST = """
            {
              "email": "david@example.com",
              "username": "David",
              "password": "bici verde"
            }
            """;

    private static final String VALID_LOGIN_REQUEST = """
            {
              "email": "david@example.com",
              "password": "bici verde"
            }
            """;

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private RegistrationService registrationService;

    @MockitoBean
    private LoginService loginService;

    @MockitoBean
    private JwtService jwtService;

    @Test
    void validRegistrationReturns201AndOnlyPublicData() throws Exception {
        given(registrationService.register(any(RegisterRequest.class)))
                .willReturn(new RegisterResponse(42L, "david"));

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(VALID_REQUEST))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(42))
                .andExpect(jsonPath("$.username").value("david"))
                .andExpect(jsonPath("$.email").doesNotExist())
                .andExpect(jsonPath("$.passwordHash").doesNotExist());
    }

    static Stream<String> invalidRegistrations() {
        return Stream.of(
                VALID_REQUEST.replace("david@example.com", "not-an-email"),
                VALID_REQUEST.replace("David", "ab"),
                VALID_REQUEST.replace("David", "david bike"),
                VALID_REQUEST.replace("bici verde", "short")
        );
    }

    @ParameterizedTest
    @MethodSource("invalidRegistrations")
    void invalidRegistrationReturns400(String body) throws Exception {
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").isNotEmpty());
    }

    @Test
    void duplicateAccountReturns409() throws Exception {
        given(registrationService.register(any(RegisterRequest.class)))
                .willThrow(new AccountAlreadyExistsException());

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(VALID_REQUEST))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail")
                        .value("Email or username is already in use"));
    }

    @Test
    void validLoginReturns200AndBearerToken() throws Exception {
        given(loginService.login(any(LoginRequest.class)))
                .willReturn(new LoginResponse("signed.jwt.token", "Bearer"));

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(VALID_LOGIN_REQUEST))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").value("signed.jwt.token"))
                .andExpect(jsonPath("$.tokenType").value("Bearer"));
    }

    static Stream<String> invalidLogins() {
        return Stream.of(
                VALID_LOGIN_REQUEST.replace("david@example.com", "not-an-email"),
                VALID_LOGIN_REQUEST.replace("david@example.com", ""),
                VALID_LOGIN_REQUEST.replace("bici verde", "")
        );
    }

    @ParameterizedTest
    @MethodSource("invalidLogins")
    void invalidLoginRequestReturns400(String body) throws Exception {
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").isNotEmpty());
    }

    @Test
    void invalidCredentialsReturn401() throws Exception {
        given(loginService.login(any(LoginRequest.class)))
                .willThrow(new InvalidCredentialsException());

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(VALID_LOGIN_REQUEST))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.detail").value("Invalid email or password"));
    }
}

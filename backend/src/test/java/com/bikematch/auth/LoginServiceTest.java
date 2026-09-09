package com.bikematch.auth;

import com.bikematch.auth.api.LoginRequest;
import com.bikematch.auth.api.LoginResponse;
import com.bikematch.user.Role;
import com.bikematch.user.User;
import com.bikematch.user.UserRepository;

import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class LoginServiceTest {

    private static final String USER_NOT_FOUND_PASSWORD_HASH = "dummy-bcrypt-hash";

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtService jwtService;

    private LoginService loginService;
    private User user;

    @BeforeEach
    void setUp() {
        given(passwordEncoder.encode("user-not-found"))
                .willReturn(USER_NOT_FOUND_PASSWORD_HASH);
        loginService = new LoginService(userRepository, passwordEncoder, jwtService);
        user = new User("david@example.com", "david", "bcrypt-hash", Role.USER);
    }

    @Test
    void validCredentialsReturnBearerToken() {
        given(userRepository.findByEmail("david@example.com")).willReturn(Optional.of(user));
        given(passwordEncoder.matches("bici verde", "bcrypt-hash")).willReturn(true);
        given(jwtService.generateToken(user)).willReturn("signed.jwt.token");

        LoginResponse response = loginService.login(new LoginRequest(
                "  DAVID@Example.COM  ",
                "bici verde"
        ));

        assertEquals("signed.jwt.token", response.accessToken());
        assertEquals("Bearer", response.tokenType());
        verify(userRepository).findByEmail("david@example.com");
    }

    @Test
    void unknownEmailReturnsGenericInvalidCredentials() {
        given(userRepository.findByEmail("missing@example.com")).willReturn(Optional.empty());
        given(passwordEncoder.matches("bici verde", USER_NOT_FOUND_PASSWORD_HASH))
                .willReturn(false);

        assertThrows(
                InvalidCredentialsException.class,
                () -> loginService.login(new LoginRequest(
                        "missing@example.com",
                        "bici verde"
                ))
        );

        verify(passwordEncoder).matches("bici verde", USER_NOT_FOUND_PASSWORD_HASH);
        verify(jwtService, never()).generateToken(user);
    }

    @Test
    void wrongPasswordReturnsGenericInvalidCredentials() {
        given(userRepository.findByEmail("david@example.com")).willReturn(Optional.of(user));
        given(passwordEncoder.matches("wrong password", "bcrypt-hash")).willReturn(false);

        assertThrows(
                InvalidCredentialsException.class,
                () -> loginService.login(new LoginRequest(
                        "david@example.com",
                        "wrong password"
                ))
        );

        verify(jwtService, never()).generateToken(user);
    }
}

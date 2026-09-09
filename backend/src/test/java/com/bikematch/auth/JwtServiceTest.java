package com.bikematch.auth;

import com.bikematch.user.Role;
import com.bikematch.user.User;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.io.Encoders;
import io.jsonwebtoken.security.Keys;

import java.time.Duration;
import java.time.Instant;
import java.util.Date;

import javax.crypto.SecretKey;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

class JwtServiceTest {

    private static final Duration EXPIRATION = Duration.ofHours(8);
    private static final String TEST_SECRET = Encoders.BASE64.encode(
            Jwts.SIG.HS256.key().build().getEncoded()
    );

    private JwtService jwtService;
    private User user;

    @BeforeEach
    void setUp() {
        jwtService = new JwtService(TEST_SECRET, EXPIRATION);
        user = mock(User.class);
        given(user.getId()).willReturn(42L);
        given(user.getUsername()).willReturn("david");
        given(user.getRole()).willReturn(Role.USER);
    }

    @Test
    void generatedTokenContainsIdentityRoleAndConfiguredExpiration() {
        Claims claims = jwtService.parseToken(jwtService.generateToken(user));

        assertEquals("42", claims.getSubject());
        assertEquals("david", claims.get("username", String.class));
        assertEquals("USER", claims.get("role", String.class));
        assertEquals(
                EXPIRATION.toMillis(),
                claims.getExpiration().getTime() - claims.getIssuedAt().getTime()
        );
    }

    @Test
    void manipulatedTokenIsRejected() {
        String token = jwtService.generateToken(user);
        int signatureStart = token.lastIndexOf('.') + 1;
        char replacement = token.charAt(signatureStart) == 'A' ? 'B' : 'A';
        String manipulatedToken = token.substring(0, signatureStart)
                + replacement
                + token.substring(signatureStart + 1);

        assertThrows(JwtException.class, () -> jwtService.parseToken(manipulatedToken));
    }

    @Test
    void expiredTokenIsRejected() {
        SecretKey signingKey = Keys.hmacShaKeyFor(Decoders.BASE64.decode(TEST_SECRET));
        String expiredToken = Jwts.builder()
                .subject("42")
                .expiration(Date.from(Instant.now().minusSeconds(1)))
                .signWith(signingKey)
                .compact();

        assertThrows(ExpiredJwtException.class, () -> jwtService.parseToken(expiredToken));
    }
}

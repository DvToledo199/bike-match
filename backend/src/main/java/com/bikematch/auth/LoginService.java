package com.bikematch.auth;

import com.bikematch.auth.api.LoginRequest;
import com.bikematch.auth.api.LoginResponse;
import com.bikematch.user.User;
import com.bikematch.user.UserRepository;

import java.util.Locale;
import java.util.Optional;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class LoginService {

    private static final String USER_NOT_FOUND_PASSWORD = "user-not-found";

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final String userNotFoundPasswordHash;

    public LoginService(
            UserRepository userRepository,
            PasswordEncoder passwordEncoder,
            JwtService jwtService
    ) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.userNotFoundPasswordHash = passwordEncoder.encode(USER_NOT_FOUND_PASSWORD);
    }

    public LoginResponse login(LoginRequest request) {
        String email = request.email().strip().toLowerCase(Locale.ROOT);

        Optional<User> user = userRepository.findByEmail(email);
        String passwordHash = user
                .map(User::getPasswordHash)
                .orElse(userNotFoundPasswordHash);

        if (!passwordEncoder.matches(request.password(), passwordHash) || user.isEmpty()) {
            throw new InvalidCredentialsException();
        }

        String accessToken = jwtService.generateToken(user.orElseThrow());
        return new LoginResponse(accessToken, "Bearer");
    }
}

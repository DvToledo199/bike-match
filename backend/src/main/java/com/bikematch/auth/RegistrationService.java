package com.bikematch.auth;

import com.bikematch.auth.api.RegisterRequest;
import com.bikematch.auth.api.RegisterResponse;
import com.bikematch.user.Role;
import com.bikematch.user.User;
import com.bikematch.user.UserRepository;

import java.util.Locale;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class RegistrationService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public RegistrationService(
            UserRepository userRepository,
            PasswordEncoder passwordEncoder
    ) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    public RegisterResponse register(RegisterRequest request) {
        String email = request.email().strip().toLowerCase(Locale.ROOT);
        String username = request.username().toLowerCase(Locale.ROOT);

        if (userRepository.existsByEmail(email)
                || userRepository.existsByUsernameIgnoreCase(username)) {
            throw new AccountAlreadyExistsException();
        }

        String passwordHash = passwordEncoder.encode(request.password());

        User user = new User(email, username, passwordHash, Role.USER);
        try {
            User savedUser = userRepository.saveAndFlush(user);
            return new RegisterResponse(savedUser.getId(), savedUser.getUsername());
        } catch (DataIntegrityViolationException exception) {
            throw new AccountAlreadyExistsException(exception);
        }
    }
}

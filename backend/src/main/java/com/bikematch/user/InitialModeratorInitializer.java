package com.bikematch.user;

import java.util.Locale;
import java.util.Optional;
import java.util.regex.Pattern;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

/** Creates the optional first moderator from ignored local environment variables. */
@Component
public class InitialModeratorInitializer implements ApplicationRunner {

    private static final Pattern BCRYPT_HASH = Pattern.compile(
            "\\$2[aby]\\$\\d{2}\\$[./A-Za-z0-9]{53}"
    );

    private final UserRepository userRepository;
    private final String email;
    private final String username;
    private final String passwordHash;

    public InitialModeratorInitializer(
            UserRepository userRepository,
            @Value("${app.initial-moderator.email:}") String email,
            @Value("${app.initial-moderator.username:}") String username,
            @Value("${app.initial-moderator.password-hash:}") String passwordHash
    ) {
        this.userRepository = userRepository;
        this.email = email;
        this.username = username;
        this.passwordHash = passwordHash;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (email.isBlank() && username.isBlank() && passwordHash.isBlank()) {
            return;
        }
        if (email.isBlank() || username.isBlank() || passwordHash.isBlank()) {
            throw new IllegalStateException(
                    "Initial moderator requires email, username and BCrypt password hash"
            );
        }
        if (!BCRYPT_HASH.matcher(passwordHash).matches()) {
            throw new IllegalStateException("Initial moderator password must be a BCrypt hash");
        }

        String normalizedEmail = email.strip().toLowerCase(Locale.ROOT);
        String normalizedUsername = username.strip().toLowerCase(Locale.ROOT);
        Optional<User> byEmail = userRepository.findByEmail(normalizedEmail);
        Optional<User> byUsername = userRepository.findByUsernameIgnoreCase(normalizedUsername);

        if (byEmail.isEmpty() && byUsername.isEmpty()) {
            userRepository.save(new User(
                    normalizedEmail,
                    normalizedUsername,
                    passwordHash,
                    Role.MODERATOR
            ));
            return;
        }

        if (byEmail.isPresent()
                && byUsername.isPresent()
                && byEmail.get().getId().equals(byUsername.get().getId())
                && byEmail.get().getRole() == Role.MODERATOR) {
            return;
        }

        throw new IllegalStateException("Initial moderator conflicts with an existing account");
    }
}

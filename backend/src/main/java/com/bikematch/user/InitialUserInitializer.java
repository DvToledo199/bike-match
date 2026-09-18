package com.bikematch.user;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;

import java.util.Locale;
import java.util.Optional;
import java.util.regex.Pattern;

/**
 * Creates an optional initial account with the given role, from local environment
 * variables that Git ignores. It runs once, when the application starts.
 */
public class InitialUserInitializer implements ApplicationRunner {

    private static final Pattern BCRYPT_HASH = Pattern.compile(
            "\\$2[aby]\\$\\d{2}\\$[./A-Za-z0-9]{53}"
    );

    private final UserRepository userRepository;
    private final String email;
    private final String username;
    private final String passwordHash;
    private final Role role;

    public InitialUserInitializer(
            UserRepository userRepository,
            String email,
            String username,
            String passwordHash,
            Role role
    ) {
        this.userRepository = userRepository;
        this.email = email;
        this.username = username;
        this.passwordHash = passwordHash;
        this.role = role;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (email.isBlank() && username.isBlank() && passwordHash.isBlank()) {
            return;
        }
        if (email.isBlank() || username.isBlank() || passwordHash.isBlank()) {
            throw new IllegalStateException(
                    "An initial account requires email, username and BCrypt password hash"
            );
        }
        if (!BCRYPT_HASH.matcher(passwordHash).matches()) {
            throw new IllegalStateException("An initial account password must be a BCrypt hash");
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
                    role
            ));
            return;
        }

        if (byEmail.isPresent()
                && byUsername.isPresent()
                && byEmail.get().getId().equals(byUsername.get().getId())
                && byEmail.get().getRole() == role) {
            return;
        }

        throw new IllegalStateException("Initial account conflicts with an existing user");
    }
}

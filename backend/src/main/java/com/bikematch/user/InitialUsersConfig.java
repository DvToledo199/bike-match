package com.bikematch.user;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** Builds the optional initial accounts, one per role, from local environment variables. */
@Configuration
public class InitialUsersConfig {

    @Bean
    InitialUserInitializer initialModerator(
            UserRepository userRepository,
            @Value("${app.initial-moderator.email:}") String email,
            @Value("${app.initial-moderator.username:}") String username,
            @Value("${app.initial-moderator.password-hash:}") String passwordHash
    ) {
        return new InitialUserInitializer(
                userRepository, email, username, passwordHash, Role.MODERATOR);
    }

    @Bean
    InitialUserInitializer initialAdmin(
            UserRepository userRepository,
            @Value("${app.initial-admin.email:}") String email,
            @Value("${app.initial-admin.username:}") String username,
            @Value("${app.initial-admin.password-hash:}") String passwordHash
    ) {
        return new InitialUserInitializer(
                userRepository, email, username, passwordHash, Role.ADMIN);
    }
}

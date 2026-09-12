package com.bikematch.user;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.mock;

@ExtendWith(MockitoExtension.class)
class InitialModeratorInitializerTest {

    private static final String VALID_HASH = "$2a$12$" + "a".repeat(53);

    @Mock
    private UserRepository userRepository;

    @Test
    void emptyConfigurationDoesNotCreateAnAccount() {
        new InitialModeratorInitializer(userRepository, "", "", "").run(null);

        verifyNoInteractions(userRepository);
    }

    @Test
    void validConfigurationCreatesModeratorWithOnlyAHash() {
        given(userRepository.findByEmail("moderator@bikematch.test")).willReturn(Optional.empty());
        given(userRepository.findByUsernameIgnoreCase("moderator")).willReturn(Optional.empty());

        new InitialModeratorInitializer(
                userRepository,
                " MODERATOR@BikeMatch.test ",
                " Moderator ",
                VALID_HASH
        ).run(null);

        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(captor.capture());
        User user = captor.getValue();
        assertEquals("moderator@bikematch.test", user.getEmail());
        assertEquals("moderator", user.getUsername());
        assertEquals(VALID_HASH, user.getPasswordHash());
        assertEquals(Role.MODERATOR, user.getRole());
    }

    @Test
    void partialConfigurationFailsInsteadOfCreatingAnUnsafeAccount() {
        var initializer = new InitialModeratorInitializer(
                userRepository,
                "moderator@bikematch.test",
                "moderator",
                ""
        );

        assertThrows(IllegalStateException.class, () -> initializer.run(null));
        verify(userRepository, never()).save(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void nonBcryptPasswordIsRejected() {
        var initializer = new InitialModeratorInitializer(
                userRepository,
                "moderator@bikematch.test",
                "moderator",
                "plain-password"
        );

        assertThrows(IllegalStateException.class, () -> initializer.run(null));
        verify(userRepository, never()).save(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void existingSameModeratorMakesInitializationIdempotent() {
        User existing = mock(User.class);
        given(existing.getId()).willReturn(7L);
        given(existing.getRole()).willReturn(Role.MODERATOR);
        given(userRepository.findByEmail("moderator@bikematch.test")).willReturn(Optional.of(existing));
        given(userRepository.findByUsernameIgnoreCase("moderator")).willReturn(Optional.of(existing));

        new InitialModeratorInitializer(
                userRepository,
                "moderator@bikematch.test",
                "moderator",
                VALID_HASH
        ).run(null);

        verify(userRepository, never()).save(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void existingAccountConflictIsNotSilentlyPromoted() {
        User existingUser = mock(User.class);
        given(userRepository.findByEmail("moderator@bikematch.test")).willReturn(Optional.of(existingUser));

        var initializer = new InitialModeratorInitializer(
                userRepository,
                "moderator@bikematch.test",
                "moderator",
                VALID_HASH
        );

        assertThrows(IllegalStateException.class, () -> initializer.run(null));
        verify(userRepository, never()).save(org.mockito.ArgumentMatchers.any());
    }
}

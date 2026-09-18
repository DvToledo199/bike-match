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
class InitialUserInitializerTest {

    private static final String VALID_HASH = "$2a$12$" + "a".repeat(53);

    @Mock
    private UserRepository userRepository;

    @Test
    void emptyConfigurationDoesNotCreateAnAccount() {
        new InitialUserInitializer(userRepository, "", "", "", Role.MODERATOR).run(null);

        verifyNoInteractions(userRepository);
    }

    @Test
    void validConfigurationCreatesModeratorWithOnlyAHash() {
        given(userRepository.findByEmail("moderator@bikematch.test")).willReturn(Optional.empty());
        given(userRepository.findByUsernameIgnoreCase("moderator")).willReturn(Optional.empty());

        new InitialUserInitializer(
                userRepository,
                " MODERATOR@BikeMatch.test ",
                " Moderator ",
                VALID_HASH,
                Role.MODERATOR
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
        var initializer = new InitialUserInitializer(
                userRepository,
                "moderator@bikematch.test",
                "moderator",
                "",
                Role.MODERATOR
        );

        assertThrows(IllegalStateException.class, () -> initializer.run(null));
        verify(userRepository, never()).save(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void nonBcryptPasswordIsRejected() {
        var initializer = new InitialUserInitializer(
                userRepository,
                "moderator@bikematch.test",
                "moderator",
                "plain-password",
                Role.MODERATOR
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

        new InitialUserInitializer(
                userRepository,
                "moderator@bikematch.test",
                "moderator",
                VALID_HASH,
                Role.MODERATOR
        ).run(null);

        verify(userRepository, never()).save(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void existingAccountConflictIsNotSilentlyPromoted() {
        User existingUser = mock(User.class);
        given(userRepository.findByEmail("moderator@bikematch.test")).willReturn(Optional.of(existingUser));

        var initializer = new InitialUserInitializer(
                userRepository,
                "moderator@bikematch.test",
                "moderator",
                VALID_HASH,
                Role.MODERATOR
        );

        assertThrows(IllegalStateException.class, () -> initializer.run(null));
        verify(userRepository, never()).save(org.mockito.ArgumentMatchers.any());
    }

    /** The class is no longer tied to one role: the same seeding works for an administrator. */
    @Test
    void theSameInitializerSeedsAnAdministrator() {
        given(userRepository.findByEmail("admin@bikematch.test")).willReturn(Optional.empty());
        given(userRepository.findByUsernameIgnoreCase("admin")).willReturn(Optional.empty());

        new InitialUserInitializer(
                userRepository,
                "admin@bikematch.test",
                "admin",
                VALID_HASH,
                Role.ADMIN
        ).run(null);

        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(captor.capture());
        assertEquals(Role.ADMIN, captor.getValue().getRole());
    }
}

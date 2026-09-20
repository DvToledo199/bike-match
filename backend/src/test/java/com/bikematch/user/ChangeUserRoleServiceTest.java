package com.bikematch.user;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verifyNoInteractions;

import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ChangeUserRoleServiceTest {

    private static final long ADMINISTRATOR_ID = 90L;

    @Mock
    private UserRepository userRepository;

    /** Without this rule an administrator could demote themselves and lock everyone out. */
    @Test
    void refusesToChangeYourOwnRole() {
        ChangeUserRoleService service = new ChangeUserRoleService(userRepository);

        assertThrows(CannotChangeOwnRoleException.class,
                () -> service.changeRole(ADMINISTRATOR_ID, Role.USER, ADMINISTRATOR_ID));

        // The refusal happens before touching the database.
        verifyNoInteractions(userRepository);
    }

    @Test
    void failsWhenTheAccountDoesNotExist() {
        given(userRepository.findById(404L)).willReturn(Optional.empty());
        ChangeUserRoleService service = new ChangeUserRoleService(userRepository);

        assertThrows(UserNotFoundException.class,
                () -> service.changeRole(404L, Role.MODERATOR, ADMINISTRATOR_ID));
    }

    /** A real User, not a mock, so the change is verified on the object itself. */
    @Test
    void grantsTheModeratorRoleToAnotherAccount() {
        User user = new User("rider@bikematch.test", "rider", "$2a$12$hash", Role.USER);
        given(userRepository.findById(88L)).willReturn(Optional.of(user));
        ChangeUserRoleService service = new ChangeUserRoleService(userRepository);

        service.changeRole(88L, Role.MODERATOR, ADMINISTRATOR_ID);

        assertThat(user.getRole()).isEqualTo(Role.MODERATOR);
    }

    @Test
    void removesTheModeratorRole() {
        User user = new User("rider@bikematch.test", "rider", "$2a$12$hash", Role.MODERATOR);
        given(userRepository.findById(88L)).willReturn(Optional.of(user));
        ChangeUserRoleService service = new ChangeUserRoleService(userRepository);

        service.changeRole(88L, Role.USER, ADMINISTRATOR_ID);

        assertThat(user.getRole()).isEqualTo(Role.USER);
    }
}

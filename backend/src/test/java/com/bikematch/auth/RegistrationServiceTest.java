package com.bikematch.auth;

import com.bikematch.auth.api.RegisterRequest;
import com.bikematch.auth.api.RegisterResponse;
import com.bikematch.user.Role;
import com.bikematch.user.User;
import com.bikematch.user.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class RegistrationServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    private RegistrationService registrationService;

    @BeforeEach
    void setUp() {
        registrationService = new RegistrationService(userRepository, passwordEncoder);
    }

    @Test
    void registrationNormalizesDataHashesPasswordAndCreatesAUserRole() {
        given(passwordEncoder.encode("bici verde")).willReturn("bcrypt-hash");
        given(userRepository.saveAndFlush(any(User.class)))
                .willAnswer(invocation -> invocation.getArgument(0));

        RegisterResponse response = registrationService.register(new RegisterRequest(
                "  DAVID@Example.COM  ",
                "David",
                "bici verde"
        ));

        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).saveAndFlush(userCaptor.capture());
        User savedUser = userCaptor.getValue();

        assertEquals("david@example.com", savedUser.getEmail());
        assertEquals("david", savedUser.getUsername());
        assertEquals(Role.USER, savedUser.getRole());
        assertEquals("bcrypt-hash", savedUser.getPasswordHash());
        assertNotEquals("bici verde", savedUser.getPasswordHash());
        assertEquals("david", response.username());
    }

    @Test
    void duplicateEmailStopsBeforeHashingOrSaving() {
        given(userRepository.existsByEmail("david@example.com")).willReturn(true);

        assertThrows(AccountAlreadyExistsException.class,
                () -> registrationService.register(validRequest()));

        verify(passwordEncoder, never()).encode(anyString());
        verify(userRepository, never()).saveAndFlush(any(User.class));
    }

    @Test
    void duplicateUsernameStopsBeforeHashingOrSaving() {
        given(userRepository.existsByUsernameIgnoreCase("david")).willReturn(true);

        assertThrows(AccountAlreadyExistsException.class,
                () -> registrationService.register(validRequest()));

        verify(passwordEncoder, never()).encode(anyString());
        verify(userRepository, never()).saveAndFlush(any(User.class));
    }

    @Test
    void databaseDuplicateRaceBecomesAccountAlreadyExists() {
        DataIntegrityViolationException databaseException =
                new DataIntegrityViolationException("Duplicate user");

        given(passwordEncoder.encode("bici verde")).willReturn("bcrypt-hash");
        given(userRepository.saveAndFlush(any(User.class)))
                .willThrow(databaseException);

        AccountAlreadyExistsException exception = assertThrows(
                AccountAlreadyExistsException.class,
                () -> registrationService.register(validRequest())
        );

        assertEquals(databaseException, exception.getCause());
    }

    private RegisterRequest validRequest() {
        return new RegisterRequest("david@example.com", "David", "bici verde");
    }
}

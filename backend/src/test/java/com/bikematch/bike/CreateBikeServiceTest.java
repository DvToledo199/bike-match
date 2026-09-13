package com.bikematch.bike;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.bikematch.kinematics.model.WheelConfiguration;
import com.bikematch.user.CurrentUserNotFoundException;
import com.bikematch.user.Role;
import com.bikematch.user.User;
import com.bikematch.user.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class CreateBikeServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private BikeRepository bikeRepository;

    private CreateBikeService service;

    @BeforeEach
    void setUp() {
        service = new CreateBikeService(userRepository, bikeRepository);
    }

    @Test
    void createsAPrivateBikeForTheAuthenticatedOwner() {
        User owner = new User("david@example.com", "david", "hash", Role.USER);
        given(userRepository.findById(42L)).willReturn(java.util.Optional.of(owner));
        given(bikeRepository.saveAndFlush(any(Bike.class)))
                .willAnswer(invocation -> invocation.getArgument(0));

        Bike bike = service.create(42L, validDetails());

        assertEquals(owner, bike.getOwner());
        assertEquals("Orange", bike.getBrand());
        assertEquals("Stage 6", bike.getModel());
        assertEquals(BikeCategory.ENDURO, bike.getCategory());
        assertEquals(SuspensionLayout.SINGLE_PIVOT, bike.getSuspensionLayout());
        assertEquals(BikeStatus.PRIVATE, bike.getStatus());
        verify(bikeRepository).saveAndFlush(bike);
    }

    @Test
    void missingAuthenticatedOwnerDoesNotCreateABike() {
        given(userRepository.findById(42L)).willReturn(java.util.Optional.empty());

        assertThrows(CurrentUserNotFoundException.class,
                () -> service.create(42L, validDetails()));

        verify(bikeRepository, never()).saveAndFlush(any(Bike.class));
    }

    private BikeDetails validDetails() {
        return new BikeDetails(
                "  Orange  ", "  Stage 6  ", (short) 2020, BikeCategory.ENDURO,
                SuspensionLayout.SINGLE_PIVOT, 150, 230, 65,
                WheelConfiguration.FULL_29, CassetteType.TWELVE_SPEED,
                (short) 32, (short) 50, 30);
    }
}

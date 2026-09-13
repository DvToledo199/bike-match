package com.bikematch.bike;

import com.bikematch.user.CurrentUserNotFoundException;
import com.bikematch.user.User;
import com.bikematch.user.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CreateBikeService {

    private final UserRepository userRepository;
    private final BikeRepository bikeRepository;

    public CreateBikeService(UserRepository userRepository, BikeRepository bikeRepository) {
        this.userRepository = userRepository;
        this.bikeRepository = bikeRepository;
    }

    @Transactional
    public Bike create(long ownerId, BikeDetails details) {
        User owner = userRepository.findById(ownerId)
                .orElseThrow(CurrentUserNotFoundException::new);
        Bike bike = Bike.createPrivate(owner, details);
        return bikeRepository.saveAndFlush(bike);
    }
}

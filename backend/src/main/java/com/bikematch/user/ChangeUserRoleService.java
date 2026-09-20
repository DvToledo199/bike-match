package com.bikematch.user;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ChangeUserRoleService {

    private final UserRepository userRepository;

    public ChangeUserRoleService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Transactional
    public void changeRole(long userId, Role newRole, long callerId) {
        if (userId == callerId) {
            throw new CannotChangeOwnRoleException();
        }

        User user = userRepository.findById(userId).orElseThrow(UserNotFoundException::new);
        user.changeRoleTo(newRole);
    }
}

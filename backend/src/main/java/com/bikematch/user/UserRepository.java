package com.bikematch.user;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByEmail(String email);

    Optional<User> findByUsernameIgnoreCase(String username);

    boolean existsByEmail(String email);

    boolean existsByUsernameIgnoreCase(String username);

    @Query("""
            select new com.bikematch.user.UserSummary(
                user.id,
                user.username,
                user.email,
                user.role,
                user.createdAt
            )
            from User user
            order by user.username
            """)
    List<UserSummary> findSummaries();
}

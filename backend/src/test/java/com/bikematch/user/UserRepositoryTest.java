package com.bikematch.user;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;

import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class UserRepositoryTest {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private EntityManager entityManager;

    @Test
    void summariesIncludeEachAccountsEmailInUsernameOrder() {
        User second = userRepository.saveAndFlush(new User(
                "second-summary@example.com", "summary_z", "password-hash", Role.USER));
        User first = userRepository.saveAndFlush(new User(
                "first-summary@example.com", "summary_a", "password-hash", Role.MODERATOR));
        entityManager.clear();

        var summaries = userRepository.findSummaries();

        assertThat(summaries)
                .extracting(UserSummary::id, UserSummary::username, UserSummary::email, UserSummary::role)
                .containsSubsequence(
                        tuple(first.getId(), "summary_a", "first-summary@example.com", Role.MODERATOR),
                        tuple(second.getId(), "summary_z", "second-summary@example.com", Role.USER));
        assertThat(summaries).allSatisfy(summary -> assertThat(summary.createdAt()).isNotNull());
    }
}

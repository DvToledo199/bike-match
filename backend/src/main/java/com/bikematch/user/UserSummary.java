package com.bikematch.user;

import java.time.Instant;

public record UserSummary(
        Long id,
        String username,
        Role role,
        Instant createdAt
) {
}

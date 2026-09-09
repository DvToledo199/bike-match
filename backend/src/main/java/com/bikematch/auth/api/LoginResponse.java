package com.bikematch.auth.api;

public record LoginResponse(
        String accessToken,
        String tokenType
) {
}

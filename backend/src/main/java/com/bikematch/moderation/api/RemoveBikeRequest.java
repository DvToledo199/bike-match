package com.bikematch.moderation.api;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RemoveBikeRequest(
        @NotBlank @Size(max = 500) String reason
) {
}

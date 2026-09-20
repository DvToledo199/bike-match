package com.bikematch.user.api;

import com.bikematch.user.Role;
import jakarta.validation.constraints.NotNull;

public record ChangeUserRoleRequest(@NotNull Role role) {
}

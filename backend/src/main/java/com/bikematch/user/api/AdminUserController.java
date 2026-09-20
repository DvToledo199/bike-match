package com.bikematch.user.api;

import com.bikematch.user.ChangeUserRoleService;
import com.bikematch.user.ListUsersService;
import com.bikematch.user.UserSummary;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/admin/users")
@Tag(name = "Admin", description = "Manage user accounts; requires the ADMIN role")
public class AdminUserController {

    private final ListUsersService listUsersService;
    private final ChangeUserRoleService changeUserRoleService;

    public AdminUserController(
            ListUsersService listUsersService,
            ChangeUserRoleService changeUserRoleService
    ) {
        this.listUsersService = listUsersService;
        this.changeUserRoleService = changeUserRoleService;
    }

    @GetMapping
    @Operation(summary = "List all user accounts")
    public List<UserSummary> listUsers() {
        return listUsersService.list();
    }

    @PutMapping("/{userId}/role")
    @Operation(summary = "Grant or remove the moderator role")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void changeRole(
            @AuthenticationPrincipal String authenticatedUserId,
            @PathVariable long userId,
            @Valid @RequestBody ChangeUserRoleRequest request
    ) {
        changeUserRoleService.changeRole(userId, request.role(), Long.parseLong(authenticatedUserId));
    }
}

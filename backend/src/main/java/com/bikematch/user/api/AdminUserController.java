package com.bikematch.user.api;

import com.bikematch.user.ListUsersService;
import com.bikematch.user.UserSummary;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/admin/users")
@Tag(name = "Admin", description = "Manage user accounts; requires the ADMIN role")
public class AdminUserController {
    private final ListUsersService listUsersService;

    public AdminUserController(ListUsersService listUsersService) {
        this.listUsersService = listUsersService;
    }

    @GetMapping
    @Operation(summary = "List all user accounts")
    public List<UserSummary> listUsers() {
        return listUsersService.list();
    }
}

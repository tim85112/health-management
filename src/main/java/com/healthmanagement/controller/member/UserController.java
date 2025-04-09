package com.healthmanagement.controller.member;

import com.healthmanagement.model.member.User;
import com.healthmanagement.service.member.UserService;
import com.healthmanagement.util.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/users")
@Tag(name = "User Management", description = "User management APIs")
@SecurityRequirement(name = "bearerAuth")
public class UserController {

    private final UserService userService;

    @Autowired
    public UserController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping
    @PreAuthorize("hasAuthority('admin')")
    @Operation(summary = "Get all users", description = "Retrieve a list of all users")
    public ResponseEntity<ApiResponse<List<User>>> getAllUsers() {
        List<User> users = userService.getAllUsers();
        return ResponseEntity.ok(ApiResponse.success(users));
    }

    @GetMapping("/{userId}")
    @PreAuthorize("hasAuthority('admin') or @userSecurity.isCurrentUser(#userId)")
    @Operation(summary = "Get user by ID", description = "Retrieve user information by their ID")
    public ResponseEntity<ApiResponse<User>> getUserById(@PathVariable Integer userId) {
        return userService.getUserById(userId)
                .map(user -> ResponseEntity.ok(ApiResponse.success(user)))
                .orElse(ResponseEntity.notFound().build());
    }

    @PutMapping("/{userId}")
    @PreAuthorize("hasAuthority('admin') or @userSecurity.isCurrentUser(#userId)")
    @Operation(summary = "Update user", description = "Update user information")
    public ResponseEntity<ApiResponse<User>> updateUser(@PathVariable Integer userId, @RequestBody User user) {
        User updatedUser = userService.updateUser(userId, user);
        return ResponseEntity.ok(ApiResponse.success(updatedUser));
    }

    @DeleteMapping("/{userId}")
    @PreAuthorize("hasAuthority('admin') or @userSecurity.isCurrentUser(#userId)")
    @Operation(summary = "Delete user", description = "Delete a user")
    public ResponseEntity<ApiResponse<Void>> deleteUser(@PathVariable Integer userId) {
        userService.deleteUser(userId);
        return ResponseEntity.ok(ApiResponse.success(null));
    }
}
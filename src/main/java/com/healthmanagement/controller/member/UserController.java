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
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/users")
@Tag(name = "用戶管理", description = "用戶管理相關API")
@SecurityRequirement(name = "bearerAuth")
public class UserController {

    private final UserService userService;

    @Autowired
    public UserController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping
    @PreAuthorize("hasAuthority('admin')")
    @Operation(summary = "獲取所有用戶", description = "獲取所有用戶的列表")
    public ResponseEntity<ApiResponse<List<User>>> getAllUsers() {
        List<User> users = userService.getAllUsers();
        return ResponseEntity.ok(ApiResponse.success(users));
    }

    @GetMapping("/{userId}")
    @PreAuthorize("hasAuthority('admin') or @userSecurity.isCurrentUser(#userId)")
    @Operation(summary = "根據ID獲取用戶", description = "通過用戶ID獲取用戶信息")
    public ResponseEntity<ApiResponse<User>> getUserById(@PathVariable Integer userId) {
        return userService.getUserById(userId)
                .map(user -> ResponseEntity.ok(ApiResponse.success(user)))
                .orElse(ResponseEntity.notFound().build());
    }

    @PutMapping("/{userId}")
    @PreAuthorize("hasAuthority('admin') or @userSecurity.isCurrentUser(#userId)")
    @Operation(summary = "更新用戶", description = "更新用戶信息")
    public ResponseEntity<ApiResponse<User>> updateUser(@PathVariable Integer userId, @RequestBody User user) {
        try {
            User updatedUser = userService.updateUser(userId, user);
            return ResponseEntity.ok(ApiResponse.success(updatedUser));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }
    //

    @DeleteMapping("/{userId}")
    @PreAuthorize("hasAuthority('admin') or @userSecurity.isCurrentUser(#userId)")
    @Operation(summary = "刪除用戶", description = "刪除指定用戶")
    public ResponseEntity<ApiResponse<Void>> deleteUser(@PathVariable Integer userId) {
        userService.deleteUser(userId);
        return ResponseEntity.ok(ApiResponse.success(null));
    }

    @GetMapping("/userinfo")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "獲取當前用戶信息", description = "獲取當前登錄用戶的信息")
    public ResponseEntity<ApiResponse<User>> getCurrentUser(@AuthenticationPrincipal UserDetails userDetails) {
        String email = userDetails.getUsername();
        return userService.findByEmail(email)
                .map(user -> ResponseEntity.ok(ApiResponse.success(user)))
                .orElse(ResponseEntity.notFound().build());
    }
    
    @GetMapping("/coaches")
    @PreAuthorize("hasAuthority('admin') or hasAuthority('coach')")
    @Operation(summary = "獲取所有教練列表", description = "獲取所有教練身份用戶的列表")
    public ResponseEntity<ApiResponse<List<User>>> getAllCoaches() {
        List<User> coaches = userService.getAllCoaches();
        return ResponseEntity.ok(ApiResponse.success(coaches));
    }
}
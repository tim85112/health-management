package com.healthmanagement.controller.member;

import com.healthmanagement.dto.member.LoginRequest;
import com.healthmanagement.dto.member.LoginResponse;
import com.healthmanagement.dto.member.RegisterRequest;
import com.healthmanagement.model.member.User;
import com.healthmanagement.service.member.UserService;
import com.healthmanagement.util.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/auth")
@Tag(name = "Authentication", description = "User authentication APIs")
public class AuthController {

    private final UserService userService;

    @Autowired
    public AuthController(UserService userService) {
        this.userService = userService;
    }

    @PostMapping("/register")
    @Operation(summary = "Register a new user", description = "Create a new user account with the provided information")
    public ResponseEntity<ApiResponse<User>> register(@Valid @RequestBody RegisterRequest registerRequest) {
        User user = User.builder()
                .name(registerRequest.getName())
                .email(registerRequest.getEmail())
                .passwordHash(registerRequest.getPassword())
                .gender(registerRequest.getGender())
                .bio(registerRequest.getBio())
                .build();

        User registeredUser = userService.registerUser(user);
        return ResponseEntity.ok(ApiResponse.success(registeredUser));
    }

    @PostMapping("/login")
    @Operation(summary = "User login", description = "Authenticate a user and return a JWT token")
    public ResponseEntity<ApiResponse<LoginResponse>> login(@Valid @RequestBody LoginRequest loginRequest) {
        String token = userService.loginUser(loginRequest.getEmail(), loginRequest.getPassword());

        // 从数据库获取用户角色
        String role = userService.findByEmail(loginRequest.getEmail())
                .map(User::getRole)
                .orElse("user");

        LoginResponse loginResponse = new LoginResponse(token, role);
        return ResponseEntity.ok(ApiResponse.success(loginResponse));
    }
}
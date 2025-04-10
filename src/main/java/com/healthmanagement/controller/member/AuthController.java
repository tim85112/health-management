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
@RequestMapping("/api/auth")
@Tag(name = "用戶認證", description = "用戶登錄和註冊相關API")
public class AuthController {

    private final UserService userService;

    @Autowired
    public AuthController(UserService userService) {
        this.userService = userService;
    }

    @PostMapping("/register")
    @Operation(summary = "註冊新用戶", description = "使用提供的信息創建新用戶帳號")
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
    @Operation(summary = "用戶登錄", description = "驗證用戶並返回JWT令牌")
    public ResponseEntity<ApiResponse<LoginResponse>> login(@Valid @RequestBody LoginRequest loginRequest) {
        String token = userService.loginUser(loginRequest.getEmail(), loginRequest.getPassword());

        // 從數據庫獲取用戶角色
        String role = userService.findByEmail(loginRequest.getEmail())
                .map(User::getRole)
                .orElse("user");

        LoginResponse loginResponse = new LoginResponse(token, role);
        return ResponseEntity.ok(ApiResponse.success(loginResponse));
    }
}
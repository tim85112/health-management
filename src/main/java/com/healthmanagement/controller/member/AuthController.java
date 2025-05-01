package com.healthmanagement.controller.member;

import com.healthmanagement.dto.member.ForgotPasswordRequest;
import com.healthmanagement.dto.member.LoginRequest;
import com.healthmanagement.dto.member.LoginResponse;
import com.healthmanagement.dto.member.RegisterRequest;
import com.healthmanagement.dto.member.ResetPasswordRequest;
import com.healthmanagement.dto.member.ValidateTokenRequest;
import com.healthmanagement.model.member.User;
import com.healthmanagement.service.member.ResetPasswordService;
import com.healthmanagement.service.member.UserService;
import com.healthmanagement.util.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
@Tag(name = "用戶認證", description = "用戶登錄、註冊和密碼管理相關API")
public class AuthController {

    private final UserService userService;
    private final ResetPasswordService resetPasswordService;

    @Autowired
    public AuthController(UserService userService, ResetPasswordService resetPasswordService) {
        this.userService = userService;
        this.resetPasswordService = resetPasswordService;
    }

    @PostMapping("/register")
    @Operation(summary = "註冊新用戶", description = "使用提供的信息創建新用戶帳號")
    public ResponseEntity<ApiResponse<User>> register(@Valid @RequestBody RegisterRequest registerRequest) {
        try {
            User user = User.builder()
                    .name(registerRequest.getName())
                    .email(registerRequest.getEmail())
                    .passwordHash(registerRequest.getPassword())
                    .gender(registerRequest.getGender())
                    .bio(registerRequest.getBio())
                    .consecutiveLoginDays(0)
                    .build();

            User registeredUser = userService.registerUser(user);
            return ResponseEntity.ok(ApiResponse.success(registeredUser));
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(ApiResponse.error(e.getMessage()));
        }
    }

    @PostMapping("/login")
    @Operation(summary = "用戶登錄", description = "驗證用戶並返回JWT令牌")
    public ResponseEntity<ApiResponse<LoginResponse>> login(@Valid @RequestBody LoginRequest loginRequest) {
        String token = userService.loginUser(loginRequest.getEmail(), loginRequest.getPassword());

        // 從數據庫獲取用戶角色和基本信息
        User user = userService.findByEmail(loginRequest.getEmail()).orElse(null);
        if (user != null) {
            LoginResponse loginResponse = new LoginResponse(
                    token,
                    user.getRole(),
                    user.getUserId(),
                    user.getName(),
                    user.getEmail()
            );
            return ResponseEntity.ok(ApiResponse.success(loginResponse));
        } else {
            return ResponseEntity.status(401).body(ApiResponse.error("Invalid credentials"));
        }
    }

    /**
     * 忘記密碼處理
     * 
     * @param request 忘記密碼請求
     * @return API響應
     */
    @PostMapping("/forgot-password")
    @Operation(summary = "忘記密碼", description = "發送重設密碼郵件到用戶的郵箱")
    public ResponseEntity<ApiResponse<Void>> forgotPassword(@Valid @RequestBody ForgotPasswordRequest request) {
        boolean sent = resetPasswordService.sendResetPasswordEmail(request.getEmail());

        // 不論用戶是否存在，都返回成功，避免暴露用戶信息
        return ResponseEntity.ok(ApiResponse.success(null));
    }

    /**
     * 重設密碼處理
     * 
     * @param request 重設密碼請求
     * @return API響應
     */
    @PostMapping("/reset-password")
    @Operation(summary = "重設密碼", description = "通過重設令牌更新用戶密碼")
    public ResponseEntity<ApiResponse<Void>> resetPassword(@Valid @RequestBody ResetPasswordRequest request) {
        boolean reset = resetPasswordService.resetPassword(request.getToken(), request.getPassword());

        if (reset) {
            return ResponseEntity.ok(ApiResponse.success(null));
        } else {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(ApiResponse.error("無效或已過期的令牌"));
        }
    }

    /**
     * 驗證重設密碼令牌
     * 
     * @param request 驗證令牌請求
     * @return API響應
     */
    @PostMapping("/validate-reset-token")
    @Operation(summary = "驗證重設密碼令牌", description = "檢查重設密碼令牌是否有效")
    public ResponseEntity<ApiResponse<Void>> validateResetToken(@Valid @RequestBody ValidateTokenRequest request) {
        boolean isValid = resetPasswordService.validateResetToken(request.getToken());

        if (isValid) {
            return ResponseEntity.ok(ApiResponse.success(null));
        } else {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(ApiResponse.error("無效或已過期的令牌"));
        }
    }
}
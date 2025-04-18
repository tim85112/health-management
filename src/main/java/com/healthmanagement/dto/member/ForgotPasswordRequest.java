package com.healthmanagement.dto.member;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 忘記密碼請求DTO
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ForgotPasswordRequest {

    /**
     * 用戶電子郵件地址
     */
    @NotBlank(message = "Email不能為空")
    @Email(message = "Email格式不正確")
    private String email;
}
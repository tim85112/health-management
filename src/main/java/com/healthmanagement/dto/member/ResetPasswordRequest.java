package com.healthmanagement.dto.member;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 重設密碼請求DTO
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ResetPasswordRequest {

    /**
     * 重設密碼令牌
     */
    @NotBlank(message = "令牌不能為空")
    private String token;

    /**
     * 新密碼
     */
    @NotBlank(message = "密碼不能為空")
    @Size(min = 8, message = "密碼長度至少為8個字符")
    @Pattern(regexp = "^(?=.*[a-z])(?=.*[A-Z]).*$", message = "密碼必須包含至少一個大寫和一個小寫字母")
    private String password;
}
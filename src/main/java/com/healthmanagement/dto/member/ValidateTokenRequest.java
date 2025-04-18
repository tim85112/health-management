package com.healthmanagement.dto.member;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 驗證重設密碼令牌請求DTO
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ValidateTokenRequest {

    /**
     * 重設密碼令牌
     */
    @NotBlank(message = "令牌不能為空")
    private String token;
}
package com.healthmanagement.service.member;

/**
 * 重設密碼服務接口
 */
public interface ResetPasswordService {

    /**
     * 發送重設密碼郵件
     * 
     * @param email 用戶電子郵件
     * @return 是否發送成功
     */
    boolean sendResetPasswordEmail(String email);

    /**
     * 通過重設令牌更改密碼
     * 
     * @param token       重設密碼令牌
     * @param newPassword 新密碼
     * @return 是否重設成功
     */
    boolean resetPassword(String token, String newPassword);
    
    /**
     * 驗證重設密碼令牌是否有效
     * 
     * @param token 重設密碼令牌
     * @return 令牌是否有效
     */
    boolean validateResetToken(String token);
}
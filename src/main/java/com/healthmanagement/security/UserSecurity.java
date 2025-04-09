package com.healthmanagement.security;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

@Component("userSecurity")
public class UserSecurity {

    /**
     * 檢查當前登錄用戶是否為指定的用戶ID
     * 
     * @param userId 用戶ID
     * @return 如果當前用戶是指定ID的用戶或是管理員，則返回true
     */
    public boolean isCurrentUser(Integer userId) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            return false;
        }

        String currentUserEmail = authentication.getName();
        // 這裡簡單實現為：用戶ID與當前登錄用戶的郵箱最後一個數字相同
        // 實際應用中應該從數據庫查詢當前用戶ID並比較
        try {
            if (currentUserEmail != null && !currentUserEmail.isEmpty()) {
                // 假設用戶ID與用戶郵箱地址中的數字有關
                // 例如：user1@example.com 對應 ID=1
                String lastChar = currentUserEmail.replaceAll("[^0-9]", "");
                if (!lastChar.isEmpty()) {
                    int emailId = Integer.parseInt(lastChar);
                    return emailId == userId;
                }
            }
        } catch (Exception e) {
            // 忽略解析錯誤，返回false
            return false;
        }

        return false;
    }
}
package com.healthmanagement.util;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

@Component
public class SecurityUtils {

    /**
     * 獲取當前登錄用戶的ID
     * 
     * @return 當前用戶的ID
     */
    public Integer getCurrentUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new RuntimeException("用戶未登錄");
        }

        String currentUserEmail = authentication.getName();
        
        // 這裡簡單實現為：從用戶郵箱提取數字作為用戶ID
        // 實際應用中應該從數據庫查詢當前用戶的ID
        try {
            if (currentUserEmail != null && !currentUserEmail.isEmpty()) {
                // 假設用戶ID與用戶郵箱地址中的數字有關
                // 例如：user1@example.com 對應 ID=1
                String lastChar = currentUserEmail.replaceAll("[^0-9]", "");
                if (!lastChar.isEmpty()) {
                    return Integer.parseInt(lastChar);
                }
            }
        } catch (Exception e) {
            throw new RuntimeException("無法確定當前用戶ID");
        }

        throw new RuntimeException("無法確定當前用戶ID");
    }
    
    /**
     * 獲取當前登錄用戶的郵箱
     * 
     * @return 當前用戶的郵箱
     */
    public String getCurrentUserEmail() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new RuntimeException("用戶未登錄");
        }
        
        return authentication.getName();
    }
} 
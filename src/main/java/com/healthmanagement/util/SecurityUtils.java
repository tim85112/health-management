package com.healthmanagement.util;

import com.healthmanagement.service.member.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

@Component
public class SecurityUtils {

    @Autowired
    private UserService userService;

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
        
        try {
            // 從數據庫獲取用戶信息
            return userService.findByEmail(currentUserEmail)
                    .map(user -> user.getId())
                    .orElseThrow(() -> new RuntimeException("找不到當前用戶"));
        } catch (Exception e) {
            throw new RuntimeException("無法確定當前用戶ID: " + e.getMessage());
        }
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
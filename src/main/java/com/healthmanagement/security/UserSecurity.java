package com.healthmanagement.security;

import com.healthmanagement.service.member.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

@Component("userSecurity")
public class UserSecurity {

    @Autowired
    private UserService userService;

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

        // 檢查是否為管理員
        if (authentication.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("admin"))) {
            return true;
        }

        String currentUserEmail = authentication.getName();
        
        try {
            // 從數據庫獲取當前用戶信息
            return userService.findByEmail(currentUserEmail)
                    .map(user -> userId.equals(user.getId()))
                    .orElse(false);
        } catch (Exception e) {
            // 忽略解析錯誤，返回false
            return false;
        }
    }
}
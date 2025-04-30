package com.healthmanagement.dto.member;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 管理員更新用戶資料的DTO
 * 包含管理員可以修改的所有用戶欄位
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AdminUpdateUserDTO {
    private String name; // 姓名
    private String email; // 電子郵件
    private String gender; // 性別
    private String bio; // 個人簡介
    private String role; // 用戶角色
    private Integer userPoints; // 用戶積分
    private String password; // 新密碼(可選)
}
package com.healthmanagement.dto.member;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 用於會員個人資料更新的數據傳輸對象
 * 只包含用戶可以修改的欄位：姓名、性別、個人簡介和密碼
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UpdateProfileDTO {
    private String email; // 用戶郵箱（用於身份識別）
    private String name; // 姓名
    private String gender; // 性別
    private String bio; // 個人簡介
    private String password; // 新密碼 (可選)
    private String oldPassword; // 舊密碼 (用於驗證密碼更改)
}
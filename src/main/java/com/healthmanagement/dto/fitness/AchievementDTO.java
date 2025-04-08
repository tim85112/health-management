package com.healthmanagement.dto.fitness;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

import com.healthmanagement.model.fitness.Achievements;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AchievementDTO {
    private Integer achievementId;       // 獎勳 ID
    private Integer userId;              // 用戶 ID
    private String achievementType;      // 獎勳類型（如 'Goal Success' 或 'General Achievement'）
    private String title;                // 獎勳標題
    private String description;          // 獎勳描述
    private LocalDate achievedDate;      // 獲得獎勳的日期
    
    // 這個構造方法用來將 Achievements 實體轉換為 DTO
    public AchievementDTO(Achievements achievements) {
        this.achievementId = achievements.getAchievementId();
        this.achievementType = achievements.getAchievementType();
        this.userId = achievements.getUserId();
        this.description = achievements.getDescription();
        this.achievedDate = achievements.getAchievedDate();  // 修正為 achievedDate
    }
}

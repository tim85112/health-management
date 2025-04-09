package com.healthmanagement.dto.fitness;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

import io.swagger.v3.oas.annotations.media.Schema;
import com.healthmanagement.model.fitness.Achievements;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AchievementDTO {

    private Integer achievementId;       

    @Schema(description = "用戶 ID", example = "123")
    private Integer userId;              

    @Schema(description = "獎勳類型（如 'Goal Success' 或 'General Achievement'）", example = "'Goal Success'")
    private String achievementType;     

    @Schema(description = "獎勳標題", example = "'Fitness Master'")
    private String title;                

    @Schema(description = "獎勳描述", example = "'Congratulations on achieving your fitness goals!'")
    private String description;          

    @Schema(description = "獲得獎勳的日期", example = "2025-04-01")
    private LocalDate achievedDate;      

    // 這個構造方法用來將 Achievements 實體轉換為 DTO
    public AchievementDTO(Achievements achievements) {
        this.achievementId = achievements.getAchievementId();
        this.achievementType = achievements.getAchievementType();
        this.userId = achievements.getUserId();
        this.description = achievements.getDescription();
        this.achievedDate = achievements.getAchievedDate();  
    }
}

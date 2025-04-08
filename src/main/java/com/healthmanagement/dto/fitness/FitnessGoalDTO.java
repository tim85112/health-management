package com.healthmanagement.dto.fitness;

import lombok.*;
import java.time.LocalDateTime;
import com.healthmanagement.model.fitness.FitnessGoal;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FitnessGoalDTO {
    private Integer goalId;
    private String goalType;
    private Float targetValue;
    private String unit;
    private Float currentProgress;
    private LocalDateTime startDate;
    private LocalDateTime endDate;
    private String status;
    private Integer userId;

    // 這個構造方法用來將 FitnessGoal 實體轉換為 DTO
    public FitnessGoalDTO(FitnessGoal fitnessGoal) {
        this.goalId = fitnessGoal.getGoalId();
        this.goalType = fitnessGoal.getGoalType();
        this.targetValue = fitnessGoal.getTargetValue();
        this.unit = fitnessGoal.getUnit();
        this.currentProgress = fitnessGoal.getCurrentProgress();
        this.startDate = fitnessGoal.getStartDate();
        this.endDate = fitnessGoal.getEndDate();
        this.status = fitnessGoal.getStatus();
        this.userId = fitnessGoal.getUser().getUserId(); // 假設 FitnessGoal 有關聯 User 實體
    }
}

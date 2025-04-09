package com.healthmanagement.dto.fitness;

import lombok.*;
import java.time.LocalDateTime;

import io.swagger.v3.oas.annotations.media.Schema;
import com.healthmanagement.model.fitness.FitnessGoal;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FitnessGoalDTO {

    @Schema(example = "1")
    private Integer goalId;

    @Schema(example = "'Weight Loss'")
    private String goalType;

    @Schema(example = "70.0")
    private Float targetValue;

    @Schema(example = "'kg'")
    private String unit;

    @Schema(example = "25.0")
    private Float currentProgress;

    @Schema(example = "2025-04-01T00:00:00")
    private LocalDateTime startDate;

    @Schema(example = "2025-07-01T00:00:00")
    private LocalDateTime endDate;

    @Schema(example = "'Active'")
    private String status;

    @Schema(example = "123")
    private Integer userId;

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

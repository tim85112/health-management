package com.healthmanagement.dto.fitness;

import com.healthmanagement.model.fitness.FitnessGoal;
import lombok.Getter;
import lombok.Setter;
import java.time.LocalDateTime;

@Getter
@Setter
public class FitnessGoalDTO {

	private Integer goalId;
	private Integer userId;
	private String goalType;
	private Float targetValue;
	private Float currentProgress;
	private String unit;
	private String status;
	private LocalDateTime startDate;
	private LocalDateTime endDate;

	public FitnessGoalDTO(FitnessGoal fitnessGoal) {
		this.goalId = fitnessGoal.getGoalId();
		this.userId = fitnessGoal.getUser().getUserId();
		this.goalType = fitnessGoal.getGoalType();
		this.targetValue = fitnessGoal.getTargetValue();
		this.currentProgress = fitnessGoal.getCurrentProgress();
		this.unit = fitnessGoal.getUnit();
		this.status = fitnessGoal.getStatus();
		this.startDate = fitnessGoal.getStartDate();  // 將 startDate 設置
        this.endDate = fitnessGoal.getEndDate();      // 將 endDate 設置
    
	}
}

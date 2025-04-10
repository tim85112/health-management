package com.healthmanagement.dto.fitness;

import com.healthmanagement.model.fitness.FitnessGoal;
import lombok.Getter;
import lombok.Setter;
import java.time.LocalDate;

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
	private LocalDate startDate;
	private LocalDate endDate;

	public FitnessGoalDTO(FitnessGoal fitnessGoal) {
		this.goalId = fitnessGoal.getGoalId();
		this.userId = fitnessGoal.getUser().getUserId();
		this.goalType = fitnessGoal.getGoalType();
		this.targetValue = fitnessGoal.getTargetValue();
		this.currentProgress = fitnessGoal.getCurrentProgress();
		this.unit = fitnessGoal.getUnit();
		this.status = fitnessGoal.getStatus();
		this.startDate = fitnessGoal.getStartDate();  
        this.endDate = fitnessGoal.getEndDate();      
    
	}
}

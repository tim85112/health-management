package com.healthmanagement.dto.fitness;

import com.healthmanagement.model.fitness.FitnessGoal;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import java.time.LocalDate;

@Getter
@Setter
@NoArgsConstructor
public class FitnessGoalDTO {

	private Integer goalId;
	private Integer userId;
	private String goalType;
	private Float targetValue;
	private double currentProgress;
	private String unit;
	private String status;
	private LocalDate startDate;
	private LocalDate endDate;
	private Float startWeight; 
	private Float startBodyFat; 
	private Float startMuscleMass;

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
	    this.startWeight = fitnessGoal.getStartWeight(); 
        this.startBodyFat = fitnessGoal.getStartBodyFat(); 
        this.startMuscleMass = fitnessGoal.getStartMuscleMass(); 
    }
}

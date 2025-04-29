package com.healthmanagement.dto.fitness;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class OverviewDataDTO {
	    private int totalWorkoutTime;
	    private double totalCaloriesBurned;
	    private int workoutCount;
	    private int consecutiveDays;
	}




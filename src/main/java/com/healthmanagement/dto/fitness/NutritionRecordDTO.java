package com.healthmanagement.dto.fitness;

import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@Builder
public class NutritionRecordDTO {
	private Integer recordId;
	private String foodName;
	private Integer calories;
	private Float protein;
	private Float carbs;
	private Float fats;
	private String mealtime;
	private LocalDateTime recordDate;
	private Integer userId;
	private String name; 
	
	public NutritionRecordDTO(Integer recordId, String foodName, Integer calories, Float protein, Float carbs, Float fats, String mealtime, LocalDateTime recordDate, Integer userId, String name) {
	    this.recordId = recordId;
	    this.foodName = foodName;
	    this.calories = calories;
	    this.protein = protein;
	    this.carbs = carbs;
	    this.fats = fats;
	    this.mealtime = mealtime;
	    this.recordDate = recordDate;
	    this.userId = userId;
	    this.name = name; 
	}
}

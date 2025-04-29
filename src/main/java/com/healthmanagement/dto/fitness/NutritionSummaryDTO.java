package com.healthmanagement.dto.fitness;

import java.util.List;
import java.util.Map;

import lombok.Data;

@Data
public class NutritionSummaryDTO {
    private List<DailyCalories> dailyCalories;
    private TotalMacros totalMacros;
    private Map<String, Double> caloriesByMealtime;

    @Data
    public static class DailyCalories {
        private String date;
        private Double calories;
    }

    @Data
    public static class TotalMacros {
        private Double protein;
        private Double carbs;
        private Double fats;
    }
}
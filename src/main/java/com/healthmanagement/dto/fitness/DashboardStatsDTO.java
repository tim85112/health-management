package com.healthmanagement.dto.fitness;


import lombok.Data;

@Data
public class DashboardStatsDTO {
    private Integer totalUsers;
    private Integer totalWorkouts;
    private Integer totalWorkoutMinutes;
    private Double totalCaloriesBurned;
    private Integer activeUsersThisWeek;
}
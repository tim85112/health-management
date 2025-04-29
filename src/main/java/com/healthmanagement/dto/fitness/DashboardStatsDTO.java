package com.healthmanagement.dto.fitness;


import lombok.Data;

import java.time.Year;

@Data
public class DashboardStatsDTO {
    private Integer totalUsers;
    private Integer totalWorkouts;
    private Integer totalWorkoutMinutes;
    private Double totalCaloriesBurned;
    private Integer activeUsersThisWeek;
    private Integer activeUsersThisMonth;
    private Integer activeUsersThisYear;
}
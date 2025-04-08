package com.healthmanagement.service.fitness;

import java.util.List;

import com.healthmanagement.dto.fitness.AchievementDTO;
import com.healthmanagement.dto.fitness.FitnessGoalDTO;

public interface FitnessGoalService {

    FitnessGoalDTO createFitnessGoal(FitnessGoalDTO fitnessGoalDTO);

    FitnessGoalDTO updateFitnessGoal(Integer goalId, FitnessGoalDTO fitnessGoalDTO);

    void deleteFitnessGoal(Integer goalId);

    FitnessGoalDTO getFitnessGoalById(Integer goalId);

    List<FitnessGoalDTO> getAllFitnessGoalsByUserId(Integer userId);
    
    List<AchievementDTO> getAchievements(Integer userId); 
}


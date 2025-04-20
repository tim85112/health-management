package com.healthmanagement.service.fitness;

import com.healthmanagement.dto.fitness.FitnessGoalDTO;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface FitnessGoalService {
    FitnessGoalDTO createFitnessGoal(FitnessGoalDTO fitnessGoalDTO);
    FitnessGoalDTO updateFitnessGoal(Integer goalId, FitnessGoalDTO fitnessGoalDTO);
    void deleteFitnessGoal(Integer goalId);
    FitnessGoalDTO getFitnessGoalById(Integer goalId);
    Page<FitnessGoalDTO> getAllFitnessGoalsByUserId(Integer userId, Pageable pageable);
    Page<FitnessGoalDTO> getAllFitnessGoalsByUserName(String name, Pageable pageable);
    Page<FitnessGoalDTO> getAllFitnessGoalsByDateRange(String startDate, String endDate, Pageable pageable);
    Page<FitnessGoalDTO> getAllFitnessGoalsByUserIdAndDateRange(Integer userId, String startDate, String endDate, Pageable pageable);
    Page<FitnessGoalDTO> getAllFitnessGoalsByCriteria(Integer userId, String name, String startDate, String endDate, String goalType, String status, Pageable pageable);
    List<FitnessGoalDTO> getAllGoalsWithProgress(int userId);
    FitnessGoalDTO updateGoalProgress(Integer goalId, Double progressValue);
}
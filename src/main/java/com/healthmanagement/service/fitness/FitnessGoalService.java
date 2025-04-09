package com.healthmanagement.service.fitness;

import com.healthmanagement.dto.fitness.FitnessGoalDTO;
import java.util.List;

public interface FitnessGoalService {
	FitnessGoalDTO createFitnessGoal(FitnessGoalDTO fitnessGoalDTO);

	FitnessGoalDTO updateFitnessGoal(Integer goalId, FitnessGoalDTO fitnessGoalDTO);

	void deleteFitnessGoal(Integer goalId);

	FitnessGoalDTO getFitnessGoalById(Integer goalId);

	List<FitnessGoalDTO> getAllFitnessGoalsByUserId(Integer userId);

	List<FitnessGoalDTO> getFitnessGoalByUserId(Integer userId);

}

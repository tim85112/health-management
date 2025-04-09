package com.healthmanagement.service.fitness;

import com.healthmanagement.dao.fitness.FitnessGoalDAO;
import com.healthmanagement.dao.member.UserDAO;
import com.healthmanagement.dto.fitness.FitnessGoalDTO;
import com.healthmanagement.model.fitness.FitnessGoal;
import com.healthmanagement.model.member.User;
import com.healthmanagement.service.fitness.FitnessGoalService;

import lombok.RequiredArgsConstructor;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@RequiredArgsConstructor
@Service
public class FitnessGoalServiceImpl implements FitnessGoalService {

	private final FitnessGoalDAO fitnessGoalRepo;
	private final UserDAO userRepo;

    @Override
    public FitnessGoalDTO createFitnessGoal(FitnessGoalDTO fitnessGoalDTO) {
        User user = userRepo.findById(fitnessGoalDTO.getUserId())
                .orElseThrow(() -> new RuntimeException("User not found"));

        FitnessGoal fitnessGoal = new FitnessGoal();
        fitnessGoal.setUser(user);  // 設置 User 實體
        fitnessGoal.setGoalType(fitnessGoalDTO.getGoalType());
        fitnessGoal.setTargetValue(fitnessGoalDTO.getTargetValue());
        fitnessGoal.setCurrentProgress(fitnessGoalDTO.getCurrentProgress());
        fitnessGoal.setUnit(fitnessGoalDTO.getUnit());
        fitnessGoal.setStartDate(fitnessGoalDTO.getStartDate());
        fitnessGoal.setEndDate(fitnessGoalDTO.getEndDate());
        fitnessGoal.setStatus(fitnessGoalDTO.getStatus());

        FitnessGoal savedFitnessGoal = fitnessGoalRepo.save(fitnessGoal);
        return new FitnessGoalDTO(savedFitnessGoal);
    }
	@Override
	public FitnessGoalDTO updateFitnessGoal(Integer goalId, FitnessGoalDTO fitnessGoalDTO) {
		FitnessGoal fitnessGoal = fitnessGoalRepo.findById(goalId)
				.orElseThrow(() -> new IllegalArgumentException("目標ID不存在"));

		fitnessGoal.setGoalType(fitnessGoalDTO.getGoalType());
		fitnessGoal.setTargetValue(fitnessGoalDTO.getTargetValue());
		fitnessGoal.setCurrentProgress(fitnessGoalDTO.getCurrentProgress());
		fitnessGoal.setUnit(fitnessGoalDTO.getUnit());
		fitnessGoal.setStartDate(fitnessGoalDTO.getStartDate());
		fitnessGoal.setEndDate(fitnessGoalDTO.getEndDate());
		fitnessGoal.setStatus(fitnessGoalDTO.getStatus());

		fitnessGoal = fitnessGoalRepo.save(fitnessGoal);

		return new FitnessGoalDTO(fitnessGoal);
	}

	@Override
	public void deleteFitnessGoal(Integer goalId) {
		fitnessGoalRepo.deleteById(goalId);
	}

	@Override
	public FitnessGoalDTO getFitnessGoalById(Integer goalId) {
		FitnessGoal fitnessGoal = fitnessGoalRepo.findById(goalId)
				.orElseThrow(() -> new IllegalArgumentException("目標ID不存在"));
		return new FitnessGoalDTO(fitnessGoal);
	}

	@Override
	public List<FitnessGoalDTO> getAllFitnessGoalsByUserId(Integer userId) {
		List<FitnessGoal> fitnessGoals = fitnessGoalRepo.findByUserUserId(userId);
		return fitnessGoals.stream().map(FitnessGoalDTO::new).collect(Collectors.toList());
	}

	@Override
	public List<FitnessGoalDTO> getFitnessGoalByUserId(Integer userId) {
		return getAllFitnessGoalsByUserId(userId);
	}
}

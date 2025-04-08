package com.healthmanagement.service.fitness;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;

import com.healthmanagement.dao.fitness.AchievementsDAO;
import com.healthmanagement.dao.fitness.FitnessGoalDAO;
import com.healthmanagement.dto.fitness.AchievementDTO;
import com.healthmanagement.dto.fitness.FitnessGoalDTO;
import com.healthmanagement.model.fitness.Achievements;
import com.healthmanagement.model.fitness.FitnessGoal;
import com.healthmanagement.model.member.User;

import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class FitnessGoalServiceImpl implements FitnessGoalService {

    private final FitnessGoalDAO fitnessGoalRepo;
    private final AchievementsDAO achievementRepo;

    @Override
    public FitnessGoalDTO createFitnessGoal(FitnessGoalDTO fitnessGoalDTO) {
        FitnessGoal fitnessGoal = new FitnessGoal();
        fitnessGoal.setGoalType(fitnessGoalDTO.getGoalType());
        fitnessGoal.setTargetValue(fitnessGoalDTO.getTargetValue());
        fitnessGoal.setCurrentProgress(fitnessGoalDTO.getCurrentProgress());
        fitnessGoal.setUnit(fitnessGoalDTO.getUnit());
        fitnessGoal.setStartDate(fitnessGoalDTO.getStartDate());
        fitnessGoal.setEndDate(fitnessGoalDTO.getEndDate());
        fitnessGoal.setStatus(fitnessGoalDTO.getStatus());
        
        User user = new User();
        user.setUserId(fitnessGoalDTO.getUserId());  // 只設 ID 就好，不用查完整 User
        fitnessGoal.setUser(user);

        fitnessGoalRepo.save(fitnessGoal);
        return convertToDTO(fitnessGoal);
    }

    @Override
    public FitnessGoalDTO updateFitnessGoal(Integer goalId, FitnessGoalDTO fitnessGoalDTO) {
        FitnessGoal fitnessGoal = fitnessGoalRepo.findById(goalId)
        		.orElseThrow(() -> new EntityNotFoundException("Fitness Goal not found"));
        fitnessGoal.setGoalType(fitnessGoalDTO.getGoalType());
        fitnessGoal.setTargetValue(fitnessGoalDTO.getTargetValue());
        fitnessGoal.setCurrentProgress(fitnessGoalDTO.getCurrentProgress());
        fitnessGoal.setUnit(fitnessGoalDTO.getUnit());
        fitnessGoal.setStartDate(fitnessGoalDTO.getStartDate());
        fitnessGoal.setEndDate(fitnessGoalDTO.getEndDate());
        fitnessGoal.setStatus(fitnessGoalDTO.getStatus());

        fitnessGoalRepo.save(fitnessGoal);
        return convertToDTO(fitnessGoal);
    }

    @Override
    public void deleteFitnessGoal(Integer goalId) {
        fitnessGoalRepo.deleteById(goalId);
    }

    @Override
    public FitnessGoalDTO getFitnessGoalById(Integer goalId) {
        FitnessGoal fitnessGoal = fitnessGoalRepo.findById(goalId)
        		.orElseThrow(() -> new EntityNotFoundException("Fitness Goal not found"));
        return convertToDTO(fitnessGoal);
    }

    @Override
    public List<FitnessGoalDTO> getAllFitnessGoalsByUserId(Integer userId) {
        List<FitnessGoal> fitnessGoals = fitnessGoalRepo.findByUserUserId(userId);
        return fitnessGoals.stream().map(this::convertToDTO).collect(Collectors.toList());
    }

    @Override
    public List<AchievementDTO> getAchievements(Integer userId) {
        List<Achievements> achievements = achievementRepo.findByUserId(userId);
        return achievements.stream()
                .map(achievement -> new AchievementDTO(achievement))
                .collect(Collectors.toList());
    }
    
    private FitnessGoalDTO convertToDTO(FitnessGoal fitnessGoal) {
        return new FitnessGoalDTO(fitnessGoal);
    }

}

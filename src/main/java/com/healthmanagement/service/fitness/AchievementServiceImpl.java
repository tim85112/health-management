package com.healthmanagement.service.fitness;

import org.springframework.stereotype.Service;

import com.healthmanagement.dao.fitness.AchievementsDAO;
import com.healthmanagement.dto.fitness.AchievementDTO;
import com.healthmanagement.model.fitness.Achievements;

import jakarta.persistence.EntityNotFoundException;

import java.util.List;
import java.util.stream.Collectors;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class AchievementServiceImpl implements AchievementService {

	private final AchievementsDAO achievementDAO;

	@Override
	public AchievementDTO createAchievement(AchievementDTO achievementDTO) {
		Achievements achievement = new Achievements();
		achievement.setUserId(achievementDTO.getUserId());
		achievement.setAchievementType(achievementDTO.getAchievementType());
		achievement.setTitle(achievementDTO.getTitle());
		achievement.setDescription(achievementDTO.getDescription());
		achievement.setAchievedDate(achievementDTO.getAchievedDate());

		achievementDAO.save(achievement);
		return convertToDTO(achievement);
	}

	@Override
	public AchievementDTO updateAchievement(Integer achievementId, AchievementDTO achievementDTO) {
		Achievements achievement = achievementDAO.findById(achievementId)
				.orElseThrow(() -> new EntityNotFoundException("Achievement not found"));
		achievement.setAchievementType(achievementDTO.getAchievementType());
		achievement.setTitle(achievementDTO.getTitle());
		achievement.setDescription(achievementDTO.getDescription());
		achievement.setAchievedDate(achievementDTO.getAchievedDate());

		achievementDAO.save(achievement);
		return convertToDTO(achievement);
	}

	@Override
	public void deleteAchievement(Integer achievementId) {
		achievementDAO.deleteById(achievementId);
	}

	@Override
	public AchievementDTO getAchievementById(Integer achievementId) {
		Achievements achievement = achievementDAO.findById(achievementId)
				.orElseThrow(() -> new EntityNotFoundException("Achievement not found"));
		return convertToDTO(achievement);
	}

	@Override
	public List<AchievementDTO> getAllAchievementsByUserId(Integer userId) {
		List<Achievements> achievements = achievementDAO.findByUserId(userId);
		return achievements.stream().map(this::convertToDTO).collect(Collectors.toList());
	}

	// 轉換Entity到DTO
	private AchievementDTO convertToDTO(Achievements achievement) {
		return AchievementDTO.builder().achievementId(achievement.getAchievementId()).userId(achievement.getUserId())
				.achievementType(achievement.getAchievementType()).title(achievement.getTitle())
				.description(achievement.getDescription()).achievedDate(achievement.getAchievedDate()).build();
	}
}

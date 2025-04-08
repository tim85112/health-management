package com.healthmanagement.service.fitness;

import java.util.List;

import com.healthmanagement.dto.fitness.AchievementDTO;

public interface AchievementService {
	AchievementDTO createAchievement(AchievementDTO achievementDTO);

	AchievementDTO updateAchievement(Integer achievementId, AchievementDTO achievementDTO);

	void deleteAchievement(Integer achievementId);

	AchievementDTO getAchievementById(Integer achievementId);

	List<AchievementDTO> getAllAchievementsByUserId(Integer userId);
}
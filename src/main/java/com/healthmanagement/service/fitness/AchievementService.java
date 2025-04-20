package com.healthmanagement.service.fitness;

import com.healthmanagement.dto.fitness.AchievementDTO;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import java.util.List;

public interface AchievementService {
	List<AchievementDTO> getUserAchievements(Integer userId);

	void checkAndAwardAchievements(Integer userId, String triggerEvent, Object data);

	AchievementDTO addAchievement(Integer userId, String achievementType, String title, String description);

	Page<AchievementDTO> getAllAchievements(Pageable pageable); // 修改為分頁

	AchievementDTO getAchievementById(Integer achievementId);

	void deleteAchievement(Integer achievementId);

	Page<AchievementDTO> searchAchievements(Integer userId, String name, String achievementType, String title,
			String startDate, String endDate, Pageable pageable);
}
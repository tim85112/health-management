package com.healthmanagement.dao.fitness;

import org.springframework.data.jpa.repository.JpaRepository;

import com.healthmanagement.model.fitness.AchievementDefinition;
import java.util.Optional;
import java.util.List;

public interface AchievementDefinitionDAO extends JpaRepository<AchievementDefinition, Integer> {
	Optional<AchievementDefinition> findByAchievementType(String achievementType);

	List<AchievementDefinition> findByTriggerEvent(String triggerEvent);
	// 可以根據需要添加其他查詢方法

}

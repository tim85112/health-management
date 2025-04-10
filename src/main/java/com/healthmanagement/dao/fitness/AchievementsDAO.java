package com.healthmanagement.dao.fitness;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.healthmanagement.model.fitness.Achievements;

public interface AchievementsDAO extends JpaRepository<Achievements, Integer> {

	// 根據用戶 ID 獲取該用戶的所有成就
    List<Achievements> findByUserId(Integer userId);

    // 根據獎勳類型篩選
    List<Achievements> findByAchievementType(String achievementType);
}
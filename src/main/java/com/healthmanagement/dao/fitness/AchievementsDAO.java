package com.healthmanagement.dao.fitness;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor; 
import com.healthmanagement.model.fitness.Achievements;

public interface AchievementsDAO extends JpaRepository<Achievements, Integer>, JpaSpecificationExecutor<Achievements> {
    List<Achievements> findByUserId(Integer userId);
    Achievements findByUserIdAndTitle(Integer userId, String title);
    Optional<Achievements> findByUserIdAndAchievementType(Integer userId, String achievementType); // 在這裡添加這個方法
   }
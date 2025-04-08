package com.healthmanagement.dao.fitness;


import com.healthmanagement.model.fitness.FitnessGoal;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface FitnessGoalDAO extends JpaRepository<FitnessGoal, Integer> {

	// 根據用戶 ID 獲取健身目標列表
	List<FitnessGoal> findByUserUserId(Integer userId);
	
    // 根據目標 ID 查詢特定健身目標
    Optional<FitnessGoal> findById(Integer goalId);

}
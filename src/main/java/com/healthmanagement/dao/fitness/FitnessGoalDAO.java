package com.healthmanagement.dao.fitness;

import com.healthmanagement.model.fitness.FitnessGoal;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface FitnessGoalDAO extends JpaRepository<FitnessGoal, Integer> {

	// 根據用戶 ID 獲取健身目標列表
	List<FitnessGoal> findByUserUserId(Integer userId);

	// 根據目標 ID 查詢特定健身目標
	Optional<FitnessGoal> findById(Integer goalId);

	// 根據用戶姓名查詢健身目標
	@Query("SELECT fg FROM FitnessGoal fg JOIN fg.user u WHERE u.name = :name")
	List<FitnessGoal> findByUserUserName(@Param("name") String name);

	// 根據日期範圍查詢健身目標
	@Query("SELECT fg FROM FitnessGoal fg WHERE (:startDate IS NULL OR fg.startDate >= :startDate) AND (:endDate IS NULL OR fg.endDate <= :endDate)")
	List<FitnessGoal> findByStartDateGreaterThanEqualAndEndDateLessThanEqual(@Param("startDate") LocalDate startDate,
			@Param("endDate") LocalDate endDate);

	// 根據用戶 ID 和日期範圍查詢健身目標
	@Query("SELECT fg FROM FitnessGoal fg JOIN fg.user u WHERE u.userId = :userId AND (:startDate IS NULL OR fg.startDate >= :startDate) AND (:endDate IS NULL OR fg.endDate <= :endDate)")
	List<FitnessGoal> findByUserUserIdAndStartDateGreaterThanEqualAndEndDateLessThanEqual(
			@Param("userId") Integer userId, @Param("startDate") LocalDate startDate,
			@Param("endDate") LocalDate endDate);
}
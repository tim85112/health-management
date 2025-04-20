package com.healthmanagement.dao.fitness;

import com.healthmanagement.model.fitness.NutritionRecord;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface NutritionRecordDAO extends JpaRepository<NutritionRecord, Integer> {
	 @Query("SELECT nr FROM NutritionRecord nr WHERE nr.user.userId = :userId")
	    List<NutritionRecord> findByUserId(@Param("userId") Integer userId);

    @Query("SELECT nr FROM NutritionRecord nr WHERE nr.user.userId = :userId AND nr.recordDate BETWEEN :startDate AND :endDate")
    List<NutritionRecord> findByUserIdAndRecordDateBetween(
            @Param("userId") Integer userId,
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate
    );
    Page<NutritionRecord> findByUser_UserId(Integer userId, Pageable pageable);
    Page<NutritionRecord> findByUser_UserIdAndRecordDateBetween(Integer userId, LocalDateTime startDate, LocalDateTime endDate, Pageable pageable);
    Page<NutritionRecord> findByRecordDateBetween(LocalDateTime startDate, LocalDateTime endDate, Pageable pageable);
    Page<NutritionRecord> findByUser_NameContaining(String name, Pageable pageable);
    Page<NutritionRecord> findByUser_NameContainingAndRecordDateBetween(String name, LocalDateTime startDate, LocalDateTime endDate, Pageable pageable);
    Page<NutritionRecord> findByUser_UserIdAndUser_NameContaining(Integer userId, String name, Pageable pageable);
    Page<NutritionRecord> findByUser_UserIdAndUser_NameContainingAndRecordDateBetween(Integer userId, String name, LocalDateTime startDate, LocalDateTime endDate, Pageable pageable);

    
    // 新增按餐別查詢的方法
    Page<NutritionRecord> findByMealtime(String mealtime, Pageable pageable);
    Page<NutritionRecord> findByUser_UserIdAndMealtime(Integer userId, String mealtime, Pageable pageable);
    Page<NutritionRecord> findByUser_NameContainingAndMealtime(String name, String mealtime, Pageable pageable);
    Page<NutritionRecord> findByRecordDateBetweenAndMealtime(LocalDateTime startDate, LocalDateTime endDate, String mealtime, Pageable pageable);
    Page<NutritionRecord> findByUser_UserIdAndUser_NameContainingAndMealtime(Integer userId, String name, String mealtime, Pageable pageable);
    Page<NutritionRecord> findByUser_UserIdAndRecordDateBetweenAndMealtime(Integer userId, LocalDateTime startDate, LocalDateTime endDate, String mealtime, Pageable pageable);
    Page<NutritionRecord> findByUser_NameContainingAndRecordDateBetweenAndMealtime(String name, LocalDateTime startDate, LocalDateTime endDate, String mealtime, Pageable pageable);
    Page<NutritionRecord> findByUser_UserIdAndUser_NameContainingAndRecordDateBetweenAndMealtime(Integer userId, String name, LocalDateTime startDate, LocalDateTime endDate, String mealtime, Pageable pageable);

    long countByUser_UserId(Integer userId);
}

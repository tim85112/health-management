package com.healthmanagement.dao.fitness;

import com.healthmanagement.model.fitness.FitnessGoal;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;

public interface FitnessGoalDAO extends JpaRepository<FitnessGoal, Integer> {

    // 根據用戶 ID 查詢健身目標
    List<FitnessGoal> findByUserId(Integer userId);

    // 根據健身目標狀態查詢目標
    List<FitnessGoal> findByStatus(String status);

    // 新增 countByUserIdAndStatus 方法
    long countByUserIdAndStatus(Integer userId, String status);

    // 根據用戶名稱模糊查詢健身目標
    @Query("SELECT fg FROM FitnessGoal fg JOIN fg.user u WHERE u.name LIKE :name")
    Page<FitnessGoal> findByUserUserNameContaining(@Param("name") String name, Pageable pageable);

    // 根據日期範圍查詢健身目標
    @Query("SELECT fg FROM FitnessGoal fg WHERE (:startDate IS NULL OR fg.startDate >= :startDate) AND (:endDate IS NULL OR fg.endDate <= :endDate)")
    Page<FitnessGoal> findByStartDateGreaterThanEqualAndEndDateLessThanEqual(@Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate, Pageable pageable);

    // 根據用戶 ID 和日期範圍查詢健身目標
    @Query("SELECT fg FROM FitnessGoal fg JOIN fg.user u WHERE u.id = :userId AND (:startDate IS NULL OR fg.startDate >= :startDate) AND (:endDate IS NULL OR fg.endDate <= :endDate)")
    List<FitnessGoal> findByUserIdAndStartDateGreaterThanEqualAndEndDateLessThanEqual(@Param("userId") Integer userId,
            @Param("startDate") LocalDate startDate, @Param("endDate") LocalDate endDate);

    Page<FitnessGoal> findByUserIdAndStartDateGreaterThanEqualAndEndDateLessThanEqual(
            Integer userId, LocalDate startDate, LocalDate endDate, Pageable pageable);

    // 根據用戶 ID 和狀態查詢健身目標
    List<FitnessGoal> findByUserIdAndStatus(Integer userId, String status);

    // 根據 goalType 查詢健身目標
    Page<FitnessGoal> findByGoalType(String goalType, Pageable pageable);

    // 根據 goalType 和 status 查詢健身目標
    Page<FitnessGoal> findByGoalTypeAndStatus(String goalType, String status, Pageable pageable);

    // 根據用戶 ID 和 goalType 查詢健身目標
    Page<FitnessGoal> findByUserIdAndGoalType(Integer userId, String goalType, Pageable pageable);

    // 根據用戶 ID 和 status 查詢健身目標
    Page<FitnessGoal> findByUserIdAndStatus(Integer userId, String status, Pageable pageable);

    // 結合其他條件的查詢方法
    @Query("SELECT fg FROM FitnessGoal fg JOIN fg.user u " +
            "WHERE (:userId IS NULL OR u.id = :userId) " +
            "AND (:name IS NULL OR u.name LIKE :name) " +
            "AND (:startDate IS NULL OR fg.startDate >= :startDate) " +
            "AND (:endDate IS NULL OR fg.endDate <= :endDate) " +
            "AND (:goalType IS NULL OR fg.goalType = :goalType) " +
            "AND (:status IS NULL OR fg.status = :status)")
    Page<FitnessGoal> findByOptionalCriteria(@Param("userId") Integer userId,
                                             @Param("name") String name,
                                             @Param("startDate") LocalDate startDate,
                                             @Param("endDate") LocalDate endDate,
                                             @Param("goalType") String goalType,
                                             @Param("status") String status,
                                             Pageable pageable);

    // 根據用戶 ID 查詢所有健身目標
    Page<FitnessGoal> findByUserId(Integer userId, Pageable pageable);

    // 根據目標 ID 和狀態查詢
    Page<FitnessGoal> findByUserIdAndGoalTypeAndStatus(Integer userId, String goalType, String status, Pageable pageable);


}
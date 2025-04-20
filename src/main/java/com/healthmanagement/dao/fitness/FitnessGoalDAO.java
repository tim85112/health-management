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
    List<FitnessGoal> findByUserUserId(Integer userId);

    List<FitnessGoal> findByUserUserIdAndStatus(Integer userId, String status);

    // 新增的方法，用於查詢所有特定狀態的目標
    List<FitnessGoal> findByStatus(String status);

    Page<FitnessGoal> findByUserUserId(Integer userId, Pageable pageable);

    @Query("SELECT fg FROM FitnessGoal fg JOIN fg.user u WHERE u.name LIKE :name")
    Page<FitnessGoal> findByUserUserNameContaining(@Param("name") String name, Pageable pageable);

    @Query("SELECT fg FROM FitnessGoal fg WHERE (:startDate IS NULL OR fg.startDate >= :startDate) AND (:endDate IS NULL OR fg.endDate <= :endDate)")
    Page<FitnessGoal> findByStartDateGreaterThanEqualAndEndDateLessThanEqual(@Param("startDate") LocalDate startDate,
                                                                             @Param("endDate") LocalDate endDate, Pageable pageable);

    @Query("SELECT fg FROM FitnessGoal fg JOIN fg.user u WHERE u.userId = :userId AND (:startDate IS NULL OR fg.startDate >= :startDate) AND (:endDate IS NULL OR fg.endDate <= :endDate)")
    Page<FitnessGoal> findByUserUserIdAndStartDateGreaterThanEqualAndEndDateLessThanEqual(
            @Param("userId") Integer userId, @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate, Pageable pageable);

    // 新增根據 goalType 的查詢方法
    Page<FitnessGoal> findByGoalType(String goalType, Pageable pageable);

    // 你已有的根據 status 的查詢方法，用於分頁
    Page<FitnessGoal> findByStatus(String status, Pageable pageable);

    // 新增根據 goalType 和 status 的查詢方法
    Page<FitnessGoal> findByGoalTypeAndStatus(String goalType, String status, Pageable pageable);

    // 結合其他條件的查詢方法
    Page<FitnessGoal> findByUserUserIdAndGoalType(Integer userId, String goalType, Pageable pageable);

    Page<FitnessGoal> findByUserUserIdAndStatus(Integer userId, String status, Pageable pageable);

    long countByUserUserIdAndStatus(Integer userId, String status);

    @Query("SELECT fg FROM FitnessGoal fg JOIN fg.user u " +
            "WHERE (:userId IS NULL OR u.userId = :userId) " +
            "AND (:name IS NULL OR u.name LIKE :name) " +
            "AND (:startDate IS NULL OR fg.startDate >= :startDate) " +
            "AND (:endDate IS NULL OR fg.endDate <= :endDate) " +
            "AND (:goalType IS NULL OR fg.goalType = :goalType) " +
            "AND (:status IS NULL OR fg.status = :status)")
    Page<FitnessGoal> findByOptionalCriteria(@Param("userId") Integer userId, @Param("name") String name,
                                             @Param("startDate") LocalDate startDate, @Param("endDate") LocalDate endDate,
                                             @Param("goalType") String goalType, @Param("status") String status, Pageable pageable);
}
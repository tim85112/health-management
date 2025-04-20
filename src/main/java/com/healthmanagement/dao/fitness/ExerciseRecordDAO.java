package com.healthmanagement.dao.fitness;

import java.time.LocalDate;
import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor; 

import com.healthmanagement.model.fitness.ExerciseRecord;

public interface ExerciseRecordDAO extends JpaRepository<ExerciseRecord, Integer>, JpaSpecificationExecutor<ExerciseRecord> {
    List<ExerciseRecord> findByUserId(Integer userId);

    // 如果 userName 儲存在 ExerciseRecord 表中
    List<ExerciseRecord> findByUserNameContaining(String userName);
    List<ExerciseRecord> findByUserIdAndUserNameContaining(Integer userId, String userName);

    Page<ExerciseRecord> findByExerciseType(String exerciseType, Pageable pageable);
    Page<ExerciseRecord> findByExerciseDateBetween(LocalDate startDate, LocalDate endDate, Pageable pageable);
    Page<ExerciseRecord> findByExerciseTypeAndExerciseDateBetween(String exerciseType, LocalDate startDate, LocalDate endDate, Pageable pageable);

    // 你可能還需要根據 userId 和 userName 進行分頁查詢的方法
    Page<ExerciseRecord> findByUserId(Integer userId, Pageable pageable);
    Page<ExerciseRecord> findByUserNameContaining(String userName, Pageable pageable);
    Page<ExerciseRecord> findByUserIdAndUserNameContaining(Integer userId, String userName, Pageable pageable);
    
    long  countByUser_UserId(Integer userId);
}
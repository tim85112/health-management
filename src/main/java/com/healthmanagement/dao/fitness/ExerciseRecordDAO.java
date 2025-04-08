package com.healthmanagement.dao.fitness;

import java.time.LocalDate;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.healthmanagement.model.fitness.ExerciseRecord;

public interface ExerciseRecordDAO extends JpaRepository<ExerciseRecord, Integer> {


    // 根據用戶 ID 和運動日期獲取運動紀錄
    List<ExerciseRecord> findByUserIdAndExerciseDate(Integer userId, LocalDate exerciseDate);

    // 根據用戶 ID 查詢所有運動紀錄
    List<ExerciseRecord> findByUserId(Integer userId);

    // 根據運動類型和運動日期篩選紀錄
    List<ExerciseRecord> findByExerciseTypeAndExerciseDate(String exerciseType, LocalDate exerciseDate);
}
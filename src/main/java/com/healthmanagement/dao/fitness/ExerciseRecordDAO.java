package com.healthmanagement.dao.fitness;

import java.time.LocalDate;
import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.healthmanagement.model.fitness.ExerciseRecord;

public interface ExerciseRecordDAO extends JpaRepository<ExerciseRecord, Integer>, JpaSpecificationExecutor<ExerciseRecord> {
    List<ExerciseRecord> findByUserId(Integer userId);

    @Query("SELECT er, u.name FROM ExerciseRecord er JOIN er.user u WHERE er.userId = :userId")
    List<Object[]> findExerciseRecordsWithUserNameByUserId(@Param("userId") Integer userId);

    // 您之前定義的其他查詢方法，如果仍然需要，可以添加回來
    List<ExerciseRecord> findByUserNameContaining(String userName);
    List<ExerciseRecord> findByUserIdAndUserNameContaining(Integer userId, String userName);

    Page<ExerciseRecord> findByExerciseType(String exerciseType, Pageable pageable);
    Page<ExerciseRecord> findByExerciseDateBetween(LocalDate startDate, LocalDate endDate, Pageable pageable);
    Page<ExerciseRecord> findByExerciseTypeAndExerciseDateBetween(String exerciseType, LocalDate startDate, LocalDate endDate, Pageable pageable);

    // 你可能還需要根據 userId 和 userName 進行分頁查詢的方法
    Page<ExerciseRecord> findByUserId(Integer userId, Pageable pageable);
    Page<ExerciseRecord> findByUserNameContaining(String userName, Pageable pageable);
    Page<ExerciseRecord> findByUserIdAndUserNameContaining(Integer userId, String userName, Pageable pageable);

    long countByUser_Id(Integer userId);
}
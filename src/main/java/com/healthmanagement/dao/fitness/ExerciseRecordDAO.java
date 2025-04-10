package com.healthmanagement.dao.fitness;

import java.time.LocalDate;
import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.healthmanagement.model.fitness.ExerciseRecord;

public interface ExerciseRecordDAO extends JpaRepository<ExerciseRecord, Integer> {

	// 根據用戶 ID 和運動日期獲取運動紀錄
	List<ExerciseRecord> findByUserIdAndExerciseDate(Integer userId, LocalDate exerciseDate);

	// 根據用戶 ID 查詢所有運動紀錄
	List<ExerciseRecord> findByUserId(Integer userId);

	// 根據運動類型和運動日期篩選紀錄
	List<ExerciseRecord> findByExerciseTypeAndExerciseDate(String exerciseType, LocalDate exerciseDate);

	// 根據用戶 ID 和用戶姓名（模糊查詢）查詢運動紀錄
	@Query("SELECT er FROM ExerciseRecord er JOIN er.user u WHERE er.userId = :userId AND u.name LIKE %:userName%")
	List<ExerciseRecord> findByUserIdAndUserNameContaining(@Param("userId") Integer userId,
			@Param("userName") String userName);

	// 根據用戶姓名（模糊查詢）查詢所有運動紀錄
	@Query("SELECT er FROM ExerciseRecord er JOIN er.user u WHERE u.name LIKE %:userName%")
	List<ExerciseRecord> findByUserNameContaining(@Param("userName") String userName);

	// 新增的分頁查詢方法
	Page<ExerciseRecord> findAll(Pageable pageable);

	Page<ExerciseRecord> findByExerciseType(String exerciseType, Pageable pageable);

	Page<ExerciseRecord> findByExerciseDateBetween(LocalDate startDate, LocalDate endDate, Pageable pageable);

	Page<ExerciseRecord> findByExerciseTypeAndExerciseDateBetween(String exerciseType, LocalDate startDate,
			LocalDate endDate, Pageable pageable);
}
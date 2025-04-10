package com.healthmanagement.service.fitness;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import com.healthmanagement.dto.fitness.ExerciseRecordDTO;

public interface ExerciseService {
	ExerciseRecordDTO saveExerciseRecord(ExerciseRecordDTO exerciseRecordDTO);

	void deleteExerciseRecord(Integer recordId);

	List<ExerciseRecordDTO> getExerciseRecordsByUserId(Integer userId);

	ExerciseRecordDTO updateExerciseRecord(Integer recordId, ExerciseRecordDTO exerciseRecordDTO);

	// 根據用戶 ID 和姓名模糊查詢運動紀錄
	List<ExerciseRecordDTO> getExerciseRecordsByUserIdAndUserName(Integer userId, String userName);

	// 根據姓名模糊查詢所有運動紀錄
	List<ExerciseRecordDTO> getExerciseRecordsByUserName(String userName);

	Page<ExerciseRecordDTO> getAllExerciseRecords(Pageable pageable, String exerciseType, String startDate,
			String endDate);

}

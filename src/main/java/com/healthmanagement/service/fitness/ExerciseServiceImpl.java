package com.healthmanagement.service.fitness;

import java.util.List;
import java.util.stream.Collectors;

import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import com.healthmanagement.dao.fitness.BodyMetricDAO;
import com.healthmanagement.dao.fitness.ExerciseRecordDAO;
import com.healthmanagement.dao.fitness.ExerciseTypeCoefficientDAO;
import com.healthmanagement.dto.fitness.ExerciseRecordDTO;
import com.healthmanagement.model.fitness.BodyMetric;
import com.healthmanagement.model.fitness.ExerciseRecord;
import com.healthmanagement.model.fitness.ExerciseTypeCoefficient;
import com.healthmanagement.service.member.UserService;
import com.healthmanagement.model.member.User;


import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ExerciseServiceImpl implements ExerciseService {

	private final ExerciseRecordDAO exerciseRecordRepo;
	private final BodyMetricDAO bodyMetricRepo;
	private final ExerciseTypeCoefficientDAO exerciseTypeCoefficientRepo;

	@Autowired(required = false)
	private UserService userService;

	@Override
	public ExerciseRecordDTO saveExerciseRecord(ExerciseRecordDTO exerciseRecordDTO) {
		// 步驟 1: 從 BodyMetrics 中獲取用戶體重
		List<BodyMetric> bodyMetrics = bodyMetricRepo.findByUserId(exerciseRecordDTO.getUserId());
		if (bodyMetrics == null || bodyMetrics.isEmpty()) {
			throw new EntityNotFoundException("找不到用戶的身體數據。");
		}
		BodyMetric bodyMetric = bodyMetrics.get(0); // 或使用排序後的第一筆最新資料

		// 步驟 2: 根據運動類型獲取 MET 值
		ExerciseTypeCoefficient exerciseType = exerciseTypeCoefficientRepo
				.findByExerciseName(exerciseRecordDTO.getExerciseType())
				.orElseThrow(() -> new EntityNotFoundException("找不到該運動類型的 MET 值。"));

		// 步驟 3: 計算消耗的卡路里
		double weight = bodyMetric.getWeight(); // 用戶的體重
		double met = exerciseType.getMet().doubleValue(); // 運動的 MET 值
		double durationInHours = exerciseRecordDTO.getExerciseDuration() / 60.0; // 將運動時長轉換為小時

		// 計算消耗的卡路里
		double caloriesBurned = met * weight * durationInHours;

		// 設置計算後的卡路里消耗
		exerciseRecordDTO.setCaloriesBurned(caloriesBurned);

		// 步驟 4: 保存運動紀錄
		ExerciseRecord exerciseRecord = new ExerciseRecord();
		exerciseRecord.setUserId(exerciseRecordDTO.getUserId());
		exerciseRecord.setExerciseType(exerciseRecordDTO.getExerciseType());
		exerciseRecord.setExerciseDuration(exerciseRecordDTO.getExerciseDuration());
		exerciseRecord.setCaloriesBurned(caloriesBurned);
		exerciseRecord.setExerciseDate(exerciseRecordDTO.getExerciseDate());

		// 保存運動紀錄並返回 DTO
		ExerciseRecord savedRecord = exerciseRecordRepo.save(exerciseRecord);
		return toDTO(savedRecord);
	}

	@Override
	public void deleteExerciseRecord(Integer recordId) {
		exerciseRecordRepo.deleteById(recordId);
	}

	@Override
	public List<ExerciseRecordDTO> getExerciseRecordsByUserId(Integer userId) {
		List<ExerciseRecord> exerciseRecords = exerciseRecordRepo.findByUserId(userId);
		return exerciseRecords.stream().map(this::toDTO).collect(Collectors.toList());
	}
 

	@Override
	public ExerciseRecordDTO updateExerciseRecord(Integer recordId, ExerciseRecordDTO exerciseRecordDTO) {
		// 查找運動紀錄
		Optional<ExerciseRecord> existingRecord = exerciseRecordRepo.findById(recordId);
		if (existingRecord.isPresent()) {
			ExerciseRecord record = existingRecord.get();
			// 更新紀錄的各個屬性
			record.setExerciseType(exerciseRecordDTO.getExerciseType());
			record.setExerciseDuration(exerciseRecordDTO.getExerciseDuration());
			record.setCaloriesBurned(exerciseRecordDTO.getCaloriesBurned());
			record.setExerciseDate(exerciseRecordDTO.getExerciseDate());
			// 其他屬性也可以根據 DTO 進行設置

			// 保存更新後的紀錄
			exerciseRecordRepo.save(record);

			// 回傳更新後的 DTO
			return new ExerciseRecordDTO(record);
		}
		return null; // 若紀錄不存在，回傳 null
	}

	@Override
	public List<ExerciseRecordDTO> getExerciseRecordsByUserIdAndUserName(Integer userId, String userName) {
		return exerciseRecordRepo.findByUserIdAndUserNameContaining(userId, userName).stream().map(this::toDTO)
				.collect(Collectors.toList());
	}

	@Override
	public List<ExerciseRecordDTO> getExerciseRecordsByUserName(String userName) {
		if (userService == null) {
			throw new IllegalStateException("UserService is not available. Cannot query by user name.");
		}
		List<User> users = userService.findByName(userName);
		return users.stream().flatMap(user -> exerciseRecordRepo.findByUserId(user.getUserId()).stream())
				.map(this::toDTO).collect(Collectors.toList());

	}
	@Override
    public Page<ExerciseRecordDTO> getAllExerciseRecords(Pageable pageable, String exerciseType, String startDate, String endDate) {
        LocalDate startLocalDate = null;
        LocalDate endLocalDate = null;
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");

        try {
            if (startDate != null && !startDate.isEmpty()) {
                startLocalDate = LocalDate.parse(startDate, formatter);
            }
            if (endDate != null && !endDate.isEmpty()) {
                endLocalDate = LocalDate.parse(endDate, formatter);
            }
        } catch (DateTimeParseException e) {
            System.err.println("日期格式錯誤: " + e.getMessage());
            // 處理日期格式錯誤
        }

        Page<ExerciseRecord> exerciseRecordPage;
        if (exerciseType != null && !exerciseType.isEmpty() && startLocalDate != null && endLocalDate != null) {
            exerciseRecordPage = exerciseRecordRepo.findByExerciseTypeAndExerciseDateBetween(exerciseType, startLocalDate, endLocalDate, pageable);
        } else if (exerciseType != null && !exerciseType.isEmpty()) {
            exerciseRecordPage = exerciseRecordRepo.findByExerciseType(exerciseType, pageable);
        } else if (startLocalDate != null && endLocalDate != null) {
            exerciseRecordPage = exerciseRecordRepo.findByExerciseDateBetween(startLocalDate, endLocalDate, pageable);
        } else {
            exerciseRecordPage = exerciseRecordRepo.findAll(pageable);
        }
        return exerciseRecordPage.map(this::toDTO);
    }

    private ExerciseRecordDTO toDTO(ExerciseRecord exerciseRecord) {
        return new ExerciseRecordDTO(exerciseRecord);
    }

}

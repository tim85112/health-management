package com.healthmanagement.service.fitness;

import com.healthmanagement.dao.fitness.NutritionRecordDAO;
import com.healthmanagement.dao.member.UserDAO;
import com.healthmanagement.dto.fitness.NutritionRecordDTO;
import com.healthmanagement.model.fitness.NutritionRecord;
import com.healthmanagement.model.member.User;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class NutritionRecordServiceImpl implements NutritionRecordService {

	private final NutritionRecordDAO nutritionRecordRepo;
	private final UserDAO userRepo;
	private final AchievementService achievementService;

	@Override
	public NutritionRecordDTO addNutritionRecord(NutritionRecordDTO recordDTO) {
		NutritionRecord record = convertToEntity(recordDTO);
		NutritionRecord savedRecord = nutritionRecordRepo.save(record);
		long dietLogCount = nutritionRecordRepo. countByUser_UserId(recordDTO.getUserId());
		achievementService.checkAndAwardAchievements(recordDTO.getUserId(), "DIET_DATA_CREATED", (int) dietLogCount);
		return convertToDTO(savedRecord);

	}

	@Override
	public NutritionRecordDTO getNutritionRecordById(Integer recordId) {
		NutritionRecord record = nutritionRecordRepo.findById(recordId)
				.orElseThrow(() -> new EntityNotFoundException("Nutrition record not found with id: " + recordId));
		return convertToDTO(record);
	}

	@Override
	public List<NutritionRecordDTO> getAllNutritionRecords() {
		return nutritionRecordRepo.findAll().stream().map(this::convertToDTO).collect(Collectors.toList());
	}

	@Override
	public List<NutritionRecordDTO> getNutritionRecordsByUserId(Integer userId) {
		return nutritionRecordRepo.findByUserId(userId).stream().map(this::convertToDTO).collect(Collectors.toList());
	}

	@Override
	public List<NutritionRecordDTO> getNutritionRecordsByUserAndDateRange(Integer userId, LocalDateTime startDate,
			LocalDateTime endDate) {
		return nutritionRecordRepo.findByUserIdAndRecordDateBetween(userId, startDate, endDate).stream()
				.map(this::convertToDTO).collect(Collectors.toList());
	}

	@Override
	public NutritionRecordDTO updateNutritionRecord(Integer recordId, NutritionRecordDTO recordDTO) {
		NutritionRecord existingRecord = nutritionRecordRepo.findById(recordId)
				.orElseThrow(() -> new EntityNotFoundException("Nutrition record not found with id: " + recordId));
		NutritionRecord updatedRecord = convertEntityFromDTO(existingRecord, recordDTO);
		NutritionRecord savedUpdatedRecord = nutritionRecordRepo.save(updatedRecord);
		return convertToDTO(savedUpdatedRecord);
	}

	@Override
	public void deleteNutritionRecord(Integer recordId) {
		nutritionRecordRepo.deleteById(recordId);
	}

	@Override
	public Page<NutritionRecordDTO> searchNutritionRecords(Integer userId, String name, LocalDateTime startDate,
			LocalDateTime endDate, String mealtime, Pageable pageable) {
		Page<NutritionRecord> recordsPage;

		if (userId != null && name != null && startDate != null && endDate != null && mealtime != null
				&& !mealtime.isEmpty()) {
			recordsPage = nutritionRecordRepo.findByUser_UserIdAndUser_NameContainingAndRecordDateBetweenAndMealtime(
					userId, name, startDate, endDate, mealtime, pageable);
		} else if (userId != null && name != null && mealtime != null && !mealtime.isEmpty()) {
			recordsPage = nutritionRecordRepo.findByUser_UserIdAndUser_NameContainingAndMealtime(userId, name, mealtime,
					pageable);
		} else if (userId != null && startDate != null && endDate != null && mealtime != null && !mealtime.isEmpty()) {
			recordsPage = nutritionRecordRepo.findByUser_UserIdAndRecordDateBetweenAndMealtime(userId, startDate,
					endDate, mealtime, pageable);
		} else if (name != null && startDate != null && endDate != null && mealtime != null && !mealtime.isEmpty()) {
			recordsPage = nutritionRecordRepo.findByUser_NameContainingAndRecordDateBetweenAndMealtime(name, startDate,
					endDate, mealtime, pageable);
		} else if (userId != null && mealtime != null && !mealtime.isEmpty()) {
			recordsPage = nutritionRecordRepo.findByUser_UserIdAndMealtime(userId, mealtime, pageable);
		} else if (name != null && mealtime != null && !mealtime.isEmpty()) {
			recordsPage = nutritionRecordRepo.findByUser_NameContainingAndMealtime(name, mealtime, pageable);
		} else if (startDate != null && endDate != null && mealtime != null && !mealtime.isEmpty()) {
			recordsPage = nutritionRecordRepo.findByRecordDateBetweenAndMealtime(startDate, endDate, mealtime,
					pageable);
		} else if (userId != null && name != null && startDate != null && endDate != null) {
			recordsPage = nutritionRecordRepo.findByUser_UserIdAndUser_NameContainingAndRecordDateBetween(userId, name,
					startDate, endDate, pageable);
		} else if (userId != null && name != null) {
			recordsPage = nutritionRecordRepo.findByUser_UserIdAndUser_NameContaining(userId, name, pageable);
		} else if (userId != null && startDate != null && endDate != null) {
			recordsPage = nutritionRecordRepo.findByUser_UserIdAndRecordDateBetween(userId, startDate, endDate,
					pageable);
		} else if (name != null && startDate != null && endDate != null) {
			recordsPage = nutritionRecordRepo.findByUser_NameContainingAndRecordDateBetween(name, startDate, endDate,
					pageable);
		} else if (userId != null) {
			recordsPage = nutritionRecordRepo.findByUser_UserId(userId, pageable);
		} else if (name != null) {
			recordsPage = nutritionRecordRepo.findByUser_NameContaining(name, pageable);
		} else if (startDate != null && endDate != null) {
			recordsPage = nutritionRecordRepo.findByRecordDateBetween(startDate, endDate, pageable);
		} else if (mealtime != null && !mealtime.isEmpty()) {
			recordsPage = nutritionRecordRepo.findByMealtime(mealtime, pageable);
		} else {
			recordsPage = nutritionRecordRepo.findAll(pageable);
		}

		return recordsPage.map(this::convertToDTO);
	}

	private NutritionRecordDTO convertToDTO(NutritionRecord record) {
		Integer userId = null;
		String userName = null; // 新增姓名
		if (record.getUser() != null) {
			userId = record.getUser().getUserId();
			userName = record.getUser().getName(); // 獲取用戶姓名
		}
		return NutritionRecordDTO.builder().recordId(record.getRecordId()).foodName(record.getFoodName())
				.calories(record.getCalories()).protein(record.getProtein()).carbs(record.getCarbs())
				.fats(record.getFats()).mealtime(record.getMealtime()).recordDate(record.getRecordDate()).userId(userId)
				.name(userName) // 設置姓名到 DTO
				.build();
	}

	private NutritionRecord convertToEntity(NutritionRecordDTO recordDTO) {
		User user = userRepo.findById(recordDTO.getUserId())
				.orElseThrow(() -> new EntityNotFoundException("User not found with id: " + recordDTO.getUserId()));
		return NutritionRecord.builder().foodName(recordDTO.getFoodName()).calories(recordDTO.getCalories())
				.protein(recordDTO.getProtein()).carbs(recordDTO.getCarbs()).fats(recordDTO.getFats())
				.mealtime(recordDTO.getMealtime()).recordDate(recordDTO.getRecordDate()).user(user).build();
	}

	private NutritionRecord convertEntityFromDTO(NutritionRecord existingRecord, NutritionRecordDTO recordDTO) {
		User user = userRepo.findById(recordDTO.getUserId())
				.orElseThrow(() -> new EntityNotFoundException("User not found with id: " + recordDTO.getUserId()));
		existingRecord.setFoodName(recordDTO.getFoodName());
		existingRecord.setCalories(recordDTO.getCalories());
		existingRecord.setProtein(recordDTO.getProtein());
		existingRecord.setCarbs(recordDTO.getCarbs());
		existingRecord.setFats(recordDTO.getFats());
		existingRecord.setMealtime(recordDTO.getMealtime());
		existingRecord.setRecordDate(recordDTO.getRecordDate());
		existingRecord.setUser(user);
		return existingRecord;
	}
}
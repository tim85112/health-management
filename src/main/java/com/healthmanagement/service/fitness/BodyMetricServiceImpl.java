package com.healthmanagement.service.fitness;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.healthmanagement.dao.fitness.BodyMetricDAO;
import com.healthmanagement.dao.fitness.FitnessGoalDAO;
import com.healthmanagement.dto.fitness.BodyMetricDTO;
import com.healthmanagement.model.fitness.BodyMetric;
import com.healthmanagement.model.fitness.FitnessGoal;
import com.healthmanagement.model.member.User;
import com.healthmanagement.service.member.UserService;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class BodyMetricServiceImpl implements BodyMetricService {

	@Autowired
	private BodyMetricDAO bodyMetricRepo;
	@Autowired
	private final AchievementService achievementService;

	@Autowired(required = false)
	private UserService userService;

	@Autowired
	private FitnessGoalDAO fitnessGoalRepo;

	@Override
	@Transactional
	public BodyMetricDTO saveBodyMetrics(BodyMetricDTO bodyMetricDTO) {
		BodyMetric bodyMetric = new BodyMetric();
		bodyMetric.setUserId(bodyMetricDTO.getUserId());
		bodyMetric.setWeight(bodyMetricDTO.getWeight());
		bodyMetric.setBodyFat(bodyMetricDTO.getBodyFat());
		bodyMetric.setHeight(bodyMetricDTO.getHeight());
		bodyMetric.setWaistCircumference(bodyMetricDTO.getWaistCircumference());
		bodyMetric.setHipCircumference(bodyMetricDTO.getHipCircumference());
		bodyMetric.setDateRecorded(bodyMetricDTO.getDateRecorded());
		bodyMetric.setMuscleMass(bodyMetricDTO.getMuscleMass());
		bodyMetric.setBmi(calculateBMI(bodyMetricDTO.getWeight(), bodyMetricDTO.getHeight()));

		BodyMetric savedBodyMetric = bodyMetricRepo.save(bodyMetric);

		// 更新健身目標進度
		updateFitnessGoalProgress(bodyMetricDTO.getUserId(), bodyMetric);

		// 檢查並頒發身體數據相關的獎章
		Integer userId = bodyMetricDTO.getUserId();
		long bodyDataCount = bodyMetricRepo.countByUser_Id(userId); // 取得該使用者的身體數據記錄總數
		System.out.println("BodyMetricServiceImpl - saveBodyMetrics - 觸發獎章檢查 - 使用者 ID: " + userId
				+ ", 事件: BODY_DATA_CREATED, 數據: " + bodyDataCount);
		System.out.println("BodyMetricServiceImpl - saveBodyMetrics - achievementService 是否為 null: "
				+ (achievementService == null));
		achievementService.checkAndAwardAchievements(userId, "BODY_DATA_CREATED", (int) bodyDataCount);
		System.out.println("BodyMetricServiceImpl - saveBodyMetrics - 已調用 achievementService.checkAndAwardAchievements");
		return convertToDTO(savedBodyMetric, null);
	}

	@Transactional
	public void updateFitnessGoalProgress(Integer userId, BodyMetric currentBodyData) {
		System.out.println("updateFitnessGoalProgress - 開始執行，使用者 ID: " + userId + ", 時間: " + java.time.LocalDateTime.now());
		System.out.println("updateFitnessGoalProgress - 當前身體數據重量: " + (currentBodyData != null ? currentBodyData.getWeight() : "null"));

		// 查詢用戶目前進行中的健身目標
		List<FitnessGoal> activeGoals = fitnessGoalRepo.findByUserIdAndStatus(userId, "進行中");
		System.out.println("updateFitnessGoalProgress - 找到 " + activeGoals.size() + " 個進行中的目標。");

		for (FitnessGoal goal : activeGoals) {
			if ("減重".equalsIgnoreCase(goal.getGoalType())) {
				System.out.println("updateFitnessGoalProgress - 目標是減重。");
				if (goal.getTargetValue() != null && currentBodyData.getWeight() != null
						&& goal.getStartWeight() != null) {
					double startWeight = goal.getStartWeight();
					double targetValue = goal.getTargetValue();
					double currentWeight = currentBodyData.getWeight();

					System.out.println("updateFitnessGoalProgress - 起始體重: " + startWeight + ", 目標減少量: " + targetValue + ", 目前體重: " + currentWeight);

					double weightDiff = startWeight - currentWeight;
					double targetDiff = targetValue;
					double progressPercentage = 0.0;

					if (targetDiff != 0) {
						progressPercentage = Math.min(100.0, Math.max(0.0, (weightDiff / targetDiff) * 100));
					} else {
						progressPercentage = weightDiff > 0 ? 100.0 : 0.0;
					}

					System.out.println("updateFitnessGoalProgress - 計算出的進度百分比: " + progressPercentage);
					goal.setCurrentProgress(progressPercentage);
					System.out.println("updateFitnessGoalProgress - 目標 ID: " + goal.getGoalId() + ", 設定目前進度為: " + progressPercentage);

					if (progressPercentage >= 100) {
						goal.setStatus("已完成");
						System.out.println("updateFitnessGoalProgress - 目標 ID: " + goal.getGoalId() + ", 狀態更新為: 已完成");
					}

					fitnessGoalRepo.save(goal);
					System.out.println("updateFitnessGoalProgress - 目標 ID: " + goal.getGoalId() + ", 進度已保存到資料庫，目前進度: " + goal.getCurrentProgress() + ", 狀態: " + goal.getStatus());

				} else {
					System.out.println("updateFitnessGoalProgress - 減重目標的必要數據為 null，跳過進度更新。目標 ID: " + goal.getGoalId() + ", targetValue: " + goal.getTargetValue() + ", currentWeight: " + currentBodyData.getWeight() + ", startWeight: " + goal.getStartWeight());
				}
			} else if ("增肌".equalsIgnoreCase(goal.getGoalType())) {
				if (goal.getTargetValue() != null && currentBodyData.getMuscleMass() != null
						&& goal.getStartMuscleMass() != null) {
					double muscleGain = currentBodyData.getMuscleMass() - goal.getStartMuscleMass();
					double targetGain = goal.getTargetValue();
					if (targetGain != 0) {
						double progressPercentage = Math.min(200.0, Math.max(0.0, (muscleGain / targetGain) * 100));
						goal.setCurrentProgress(progressPercentage);	if (progressPercentage >= 100) {
							goal.setStatus("已完成");
						}
					} else {
						goal.setCurrentProgress(muscleGain > 0 ? 100.0 : 0.0);
						if (goal.getCurrentProgress() >= 100) {
							goal.setStatus("已完成");
						}
					}
					fitnessGoalRepo.save(goal);
				}
			} else if ("減脂".equalsIgnoreCase(goal.getGoalType())) {
				if (goal.getTargetValue() != null && currentBodyData.getBodyFat() != null
						&& goal.getStartBodyFat() != null) {
					double fatLoss = goal.getStartBodyFat() - currentBodyData.getBodyFat();
					double targetLoss =goal.getTargetValue();
					if (targetLoss != 0) {
						double progressPercentage = Math.min(100.0, Math.max(0.0, (fatLoss / targetLoss) * 100));
						goal.setCurrentProgress(progressPercentage);
						if (progressPercentage >= 100) {
							goal.setStatus("已完成");
						}
					} else {
						goal.setCurrentProgress(fatLoss > 0 ? 100.0 : 0.0);
						if (goal.getCurrentProgress() >= 100) {
							goal.setStatus("已完成");
						}
					}
					fitnessGoalRepo.save(goal);
				}
			}
			// 在這裡可以添加對其他目標類型的處理
		}
	}

	@Override
	public BodyMetricDTO calculateBMI(BodyMetricDTO bodyMetricDTO) {
		double bmi = calculateBMI(bodyMetricDTO.getWeight(), bodyMetricDTO.getHeight());
		bodyMetricDTO.setBmi(bmi);
		return bodyMetricDTO;
	}


	public boolean existsByUserId(Integer userId) {
		return bodyMetricRepo.existsByUserId(userId);
	}

	@Override
	public void deleteBodyMetric(Integer bodyMetricId) {
		bodyMetricRepo.deleteById(bodyMetricId);
	}

	@Override
	public List<BodyMetricDTO> getAllBodyMetrics() {
		return bodyMetricRepo.findAll().stream().map(bodyMetric -> convertToDTO(bodyMetric, null)).toList();
	}

	@Override
	public List<BodyMetricDTO> findByUserId(Integer userId) {
		return bodyMetricRepo.findByUserId(userId).stream().map(bodyMetric -> convertToDTO(bodyMetric, null)).toList();
	}

	@Override
	public BodyMetricDTO updateBodyMetric(Integer bodyMetricId, BodyMetricDTO bodyMetricDTO) {
		// 這裡的 bodyMetricId 是路徑參數，應該與 DTO 中的 id 一致
		if (bodyMetricDTO.getId() == null || !bodyMetricId.equals(bodyMetricDTO.getId())) {
			// 處理 DTO 中沒有 id 或 id 與路徑參數不符的情況，例如拋出異常或返回錯誤
			System.err.println("更新請求的 ID 不匹配");
			return null; // 或者拋出 IllegalArgumentException
		}

		Optional<BodyMetric> existingBodyMetricOpt = bodyMetricRepo.findById(bodyMetricId);

		if (existingBodyMetricOpt.isPresent()) {
			BodyMetric existingBodyMetric = existingBodyMetricOpt.get();
			existingBodyMetric.setWeight(bodyMetricDTO.getWeight());
			existingBodyMetric.setBodyFat(bodyMetricDTO.getBodyFat());
			existingBodyMetric.setHeight(bodyMetricDTO.getHeight());
			existingBodyMetric.setWaistCircumference(bodyMetricDTO.getWaistCircumference());
			existingBodyMetric.setHipCircumference(bodyMetricDTO.getHipCircumference());
			existingBodyMetric.setMuscleMass(bodyMetricDTO.getMuscleMass());
			existingBodyMetric.setDateRecorded(bodyMetricDTO.getDateRecorded());
			existingBodyMetric.setBmi(calculateBMI(bodyMetricDTO.getWeight(), bodyMetricDTO.getHeight()));

			BodyMetric updatedBodyMetric = bodyMetricRepo.save(existingBodyMetric);
			return convertToDTO(updatedBodyMetric, null);
		}
		return null;
	}

	@Override
	public List<BodyMetricDTO> findByUserIdAndDateRange(Integer userId, String startDate, String endDate) {
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
			return Collections.emptyList();
		}
		return bodyMetricRepo.findByUserIdAndDateRecordedBetween(userId, startLocalDate, endLocalDate).stream()
				.map(bodyMetric -> convertToDTO(bodyMetric, null)).toList();
	}

	@Override
	public List<BodyMetricDTO> findByMultipleCriteria(Integer userId, String userName, String startDate,
													  String endDate) {
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
			return List.of();
		}

		List<BodyMetric> bodyMetrics = bodyMetricRepo.findByMultipleCriteria(userId, userName, startLocalDate,
				endLocalDate);
		return bodyMetrics.stream().map(bm -> convertToDTO(bm)).collect(Collectors.toList());
	}

	@Override
	public List<BodyMetricDTO> findByUserName(String name) {
		if (userService == null) {
			throw new IllegalStateException("UserService is not available. Cannot query by user name.");
		}
		List<User> users = userService.findByName(name);
		return users.stream().flatMap(user -> bodyMetricRepo.findByUserId(user.getUserId()).stream()
				.map(bodyMetric -> convertToDTO(bodyMetric, user))).toList();
	}

	@Override
	public Page<BodyMetricDTO> findByMultipleCriteriaWithPagination(Integer userId, String userName, String startDate,
																	String endDate, Pageable pageable) {
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

		}

		Page<BodyMetric> bodyMetricPage = bodyMetricRepo.findByMultipleCriteriaPage(userId, userName, startLocalDate,
				endLocalDate, pageable);
		return bodyMetricPage.map(bodyMetric -> {
			User user = userService.findById(bodyMetric.getUserId()).orElse(null);
			return BodyMetricDTO.fromEntity(bodyMetric, user);
		});
	}

	@Override
	public Optional<BodyMetricDTO> findLatestByUserId(Integer userId) {
		return bodyMetricRepo.findTopByUserIdOrderByDateRecordedDesc(userId).map(this::convertToDTO);
	}

	// 獨立的 BMI 計算方法
	private double calculateBMI(double weight, double height) {
		return weight / (height / 100) / (height / 100); // BMI = 體重(kg) / 身高(m)^2
	}

	private BodyMetricDTO convertToDTO(BodyMetric bodyMetric) {
		return BodyMetricDTO.fromEntity(bodyMetric);
	}

	private BodyMetricDTO convertToDTO(BodyMetric bodyMetric, User user) {
		return BodyMetricDTO.fromEntity(bodyMetric, user);
	}
}

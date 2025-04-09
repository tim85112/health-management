package com.healthmanagement.service.fitness;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.healthmanagement.dao.fitness.BodyMetricDAO;
import com.healthmanagement.dto.fitness.BodyMetricDTO;
import com.healthmanagement.model.fitness.BodyMetric;
import com.healthmanagement.model.member.User;
import com.healthmanagement.service.member.UserService;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
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

	@Autowired(required = false)
	private UserService userService;

	@Override
	public BodyMetricDTO saveBodyMetrics(BodyMetricDTO bodyMetricDTO) {
		BodyMetric bodyMetric = new BodyMetric();
		bodyMetric.setUserId(bodyMetricDTO.getUserId());
		bodyMetric.setWeight(bodyMetricDTO.getWeight());
		bodyMetric.setBodyFat(bodyMetricDTO.getBodyFat());
		bodyMetric.setHeight(bodyMetricDTO.getHeight());
		bodyMetric.setWaistCircumference(bodyMetricDTO.getWaistCircumference());
		bodyMetric.setHipCircumference(bodyMetricDTO.getHipCircumference());
		bodyMetric.setDateRecorded(bodyMetricDTO.getDateRecorded());

		// 計算 BMI
		double bmi = calculateBMI(bodyMetricDTO.getWeight(), bodyMetricDTO.getHeight());
		bodyMetric.setBmi(bmi);

		// 保存到資料庫
		bodyMetricRepo.save(bodyMetric);

		// 更新並返回 DTO
		bodyMetricDTO.setBmi(bmi); // 把計算出來的 BMI 放到 DTO 中
		return bodyMetricDTO;
	}

	@Override
	public BodyMetricDTO calculateBMI(BodyMetricDTO bodyMetricDTO) {
		// 使用 BMI 計算邏輯
		double bmi = calculateBMI(bodyMetricDTO.getWeight(), bodyMetricDTO.getHeight());
		bodyMetricDTO.setBmi(bmi);
		return bodyMetricDTO;
	}

	@Override
	public void deleteBodyMetric(Integer bodyMetricId) {
		// 查找並刪除指定的 BodyMetric
		bodyMetricRepo.deleteById(bodyMetricId);
	}

	@Override
	public List<BodyMetricDTO> getAllBodyMetrics() {
		// 查詢所有身體數據
		List<BodyMetric> bodyMetrics = bodyMetricRepo.findAll();

		// 把 Entity 轉換成 DTO
		return bodyMetrics.stream().map(this::convertToDTO).toList();
	}

	@Override
	public List<BodyMetricDTO> findByUserId(Integer userId) {
		// 根據 userId 查詢所有身體數據
		List<BodyMetric> bodyMetrics = bodyMetricRepo.findByUserId(userId);

		// 轉換為 DTO 並返回
		return bodyMetrics.stream().map(this::convertToDTO).toList();
	}

	@Override
	public BodyMetricDTO updateBodyMetric(Integer bodyMetricId, BodyMetricDTO bodyMetricDTO) {
		// 查找 BodyMetric 並更新
		Optional<BodyMetric> existingBodyMetricOpt = bodyMetricRepo.findById(bodyMetricId);

		if (existingBodyMetricOpt.isPresent()) {
			BodyMetric existingBodyMetric = existingBodyMetricOpt.get();
			existingBodyMetric.setWeight(bodyMetricDTO.getWeight());
			existingBodyMetric.setBodyFat(bodyMetricDTO.getBodyFat());
			existingBodyMetric.setHeight(bodyMetricDTO.getHeight());
			existingBodyMetric.setWaistCircumference(bodyMetricDTO.getWaistCircumference());
			existingBodyMetric.setHipCircumference(bodyMetricDTO.getHipCircumference());
			existingBodyMetric.setDateRecorded(bodyMetricDTO.getDateRecorded());

			// 計算並更新 BMI
			double bmi = calculateBMI(bodyMetricDTO.getWeight(), bodyMetricDTO.getHeight());
			existingBodyMetric.setBmi(bmi);

			// 保存更新到資料庫
			bodyMetricRepo.save(existingBodyMetric);

			// 返回更新後的 DTO
			bodyMetricDTO.setBmi(bmi);
			return bodyMetricDTO;
		} else {
			// 找不到 BodyMetric，返回 null 或拋出異常
			return null;
		}
	}
	

	@Override
	public List<BodyMetricDTO> findByUserIdAndDateRange(Integer userId, String startDate, String endDate) {
		LocalDate startLocalDate = null;
		LocalDate endLocalDate = null;
		DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
		List<BodyMetric> bodyMetrics = Collections.emptyList();

		try {
			if (startDate != null && !startDate.isEmpty()) {
				startLocalDate = LocalDate.parse(startDate, formatter);
			}
			if (endDate != null && !endDate.isEmpty()) {
				endLocalDate = LocalDate.parse(endDate, formatter);
			}

			bodyMetrics = bodyMetricRepo.findByUserIdAndDateRecordedBetween(userId,
					startLocalDate != null ? startLocalDate.atStartOfDay() : null,
					endLocalDate != null ? endLocalDate.atTime(LocalTime.MAX) : null);
		} catch (DateTimeParseException e) {
			// 處理日期格式錯誤，可以記錄日誌或返回錯誤訊息
			System.err.println("日期格式錯誤: " + e.getMessage());
			return Collections.emptyList(); // 返回空列表避免後續處理錯誤
		}
		return bodyMetrics.stream().map(this::convertToDTO).toList();
	}

	@Override
	public List<BodyMetricDTO> findByMultipleCriteria(Integer userId, String userName, String startDate,
			String endDate) {
		LocalDateTime startLocalDate = null;
		LocalDateTime endLocalDate = null;
		DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");

		try {
			if (startDate != null && !startDate.isEmpty()) {
				startLocalDate = LocalDate.parse(startDate, formatter).atStartOfDay();
			}
			if (endDate != null && !endDate.isEmpty()) {
				endLocalDate = LocalDate.parse(endDate, formatter).atTime(LocalTime.MAX);
			}
		} catch (DateTimeParseException e) {
			System.err.println("日期格式錯誤: " + e.getMessage());
			return List.of();
		}

		List<BodyMetric> bodyMetrics = bodyMetricRepo.findByMultipleCriteria(userId, userName, startLocalDate,
				endLocalDate);
		return bodyMetrics.stream().map(this::convertToDTO).collect(Collectors.toList());
	}

	@Override
	public List<BodyMetricDTO> findByUserName(String name) {
		if (userService == null) {
			throw new IllegalStateException("UserService is not available. Cannot query by user name.");
		}
		List<User> users = userService.findByName(name);
		return users.stream()
				.flatMap(user -> ((List<BodyMetric>) bodyMetricRepo.findByUserId(user.getUserId())).stream())
				.map(this::convertToDTO).toList();
	}

	// 獨立的 BMI 計算方法
	private double calculateBMI(double weight, double height) {
		return weight / (height / 100) / (height / 100); // BMI = 體重(kg) / 身高(m)^2
	}

	private BodyMetricDTO convertToDTO(BodyMetric bodyMetric) {
		return BodyMetricDTO.fromEntity(bodyMetric);
	}

}

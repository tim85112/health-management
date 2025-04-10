package com.healthmanagement.service.fitness;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import com.healthmanagement.dao.fitness.BodyMetricDAO;
import com.healthmanagement.dto.fitness.BodyMetricDTO;
import com.healthmanagement.model.fitness.BodyMetric;
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
		bodyMetric.setBmi(calculateBMI(bodyMetricDTO.getWeight(), bodyMetricDTO.getHeight()));

		BodyMetric savedBodyMetric = bodyMetricRepo.save(bodyMetric);
		return convertToDTO(savedBodyMetric, null);
	}

	@Override
	public BodyMetricDTO calculateBMI(BodyMetricDTO bodyMetricDTO) {
		double bmi = calculateBMI(bodyMetricDTO.getWeight(), bodyMetricDTO.getHeight());
		bodyMetricDTO.setBmi(bmi);
		return bodyMetricDTO;
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
		return bodyMetrics.stream().map(this::convertToDTO).collect(Collectors.toList());
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
			// 處理日期格式錯誤
		}

		Page<BodyMetric> bodyMetricPage = bodyMetricRepo.findByMultipleCriteria(userId, userName, startLocalDate,
				endLocalDate, pageable);
		return bodyMetricPage.map(bodyMetric -> {
			User user = userService.findById(bodyMetric.getUserId()).orElse(null);
			return BodyMetricDTO.fromEntity(bodyMetric, user);
		});
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
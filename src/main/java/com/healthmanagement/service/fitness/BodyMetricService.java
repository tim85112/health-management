package com.healthmanagement.service.fitness;

import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import com.healthmanagement.dto.fitness.BodyMetricDTO;

public interface BodyMetricService {
	// 保存身體數據
	BodyMetricDTO saveBodyMetrics(BodyMetricDTO bodyMetricDTO);

	// 計算BMI
	BodyMetricDTO calculateBMI(BodyMetricDTO bodyMetricDTO);

	// 刪除身體數據
	void deleteBodyMetric(Integer bodyMetricId);

	// 查詢所有身體數據
	List<BodyMetricDTO> getAllBodyMetrics();

	// 根據 userId 查詢 BodyMetric
	List<BodyMetricDTO> findByUserId(Integer userId);
	
	// 根據使用者 ID 查詢最近一次記錄的身體數據
	Optional<BodyMetricDTO> findLatestByUserId(Integer userId);

	// 更新身體數據
	BodyMetricDTO updateBodyMetric(Integer bodyMetricId, BodyMetricDTO bodyMetricDTO);

	// 根據 userId 和日期範圍查詢 BodyMetric
	List<BodyMetricDTO> findByUserIdAndDateRange(Integer userId, String startDate, String endDate);

	// 根據姓名查詢 BodyMetric (需要用戶服務)
	List<BodyMetricDTO> findByUserName(String name);

	List<BodyMetricDTO> findByMultipleCriteria(Integer userId, String userName, String startDate, String endDate);

	Page<BodyMetricDTO> findByMultipleCriteriaWithPagination(Integer userId, String userName, String startDate,
			String endDate, Pageable pageable);
	
	 boolean existsByUserId(Integer userId);

}
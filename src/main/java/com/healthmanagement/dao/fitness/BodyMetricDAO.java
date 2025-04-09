package com.healthmanagement.dao.fitness;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.time.LocalDateTime;

import com.healthmanagement.model.fitness.BodyMetric;

public interface BodyMetricDAO extends JpaRepository<BodyMetric, Integer> {
	List<BodyMetric> findByUserId(Integer userId);

	List<BodyMetric> findByUserIdAndDateRecordedBetween(Integer userId, LocalDateTime startDate, LocalDateTime endDate);

	@Query("SELECT bm FROM BodyMetric bm JOIN bm.user u " + "WHERE (:userId IS NULL OR bm.userId = :userId) "
			+ "AND (:userName IS NULL OR LOWER(u.name) LIKE LOWER(CONCAT('%', :userName, '%'))) "
			+ "AND (:startDate IS NULL OR bm.dateRecorded >= :startDate) "
			+ "AND (:endDate IS NULL OR bm.dateRecorded <= :endDate)")
	List<BodyMetric> findByMultipleCriteria(@Param("userId") Integer userId, @Param("userName") String userName,
			@Param("startDate") LocalDateTime startDate, @Param("endDate") LocalDateTime endDate);
}
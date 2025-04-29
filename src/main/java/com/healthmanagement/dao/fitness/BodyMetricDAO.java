package com.healthmanagement.dao.fitness;

import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.time.LocalDate;


import com.healthmanagement.model.fitness.BodyMetric;

public interface BodyMetricDAO extends JpaRepository<BodyMetric, Integer> {
	List<BodyMetric> findByUserId(Integer userId);

	List<BodyMetric> findByUserIdAndDateRecordedBetween(Integer userId, LocalDate startDate, LocalDate endDate);
	
	Optional<BodyMetric> findTopByUserIdOrderByDateRecordedDesc(Integer userId);

	@Query("SELECT bm FROM BodyMetric bm JOIN bm.user u " + "WHERE (:userId IS NULL OR bm.userId = :userId) "
			+ "AND (:userName IS NULL OR LOWER(u.name) LIKE LOWER(CONCAT('%', :userName, '%'))) "
			+ "AND (:startDate IS NULL OR bm.dateRecorded >= :startDate) "
			+ "AND (:endDate IS NULL OR bm.dateRecorded <= :endDate)")
	List<BodyMetric> findByMultipleCriteria(@Param("userId") Integer userId, @Param("userName") String userName,
			@Param("startDate") LocalDate startDate, @Param("endDate") LocalDate endDate);

	@Query("SELECT bm FROM BodyMetric bm JOIN bm.user u " 
			+ "WHERE (:userId IS NULL OR bm.userId = :userId) "
			+ "AND (:userName IS NULL OR LOWER(u.name) LIKE LOWER(CONCAT('%', :userName, '%'))) "
			+ "AND (:startDate IS NULL OR bm.dateRecorded >= :startDate) "
			+ "AND (:endDate IS NULL OR bm.dateRecorded <= :endDate)")
	Page<BodyMetric> findByMultipleCriteriaPage(@Param("userId") Integer userId, @Param("userName") String userName,
			@Param("startDate") LocalDate startDate, @Param("endDate") LocalDate endDate, Pageable pageable);
	
	long countByUser_Id(Integer userId);
	
	boolean existsByUserId(Integer userId);

	    
}
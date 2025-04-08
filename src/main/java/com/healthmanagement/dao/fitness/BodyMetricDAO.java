package com.healthmanagement.dao.fitness;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.healthmanagement.model.fitness.BodyMetric;

public interface BodyMetricDAO extends JpaRepository<BodyMetric, Integer> {
	 List<BodyMetric> findByUserId(Integer userId);
}

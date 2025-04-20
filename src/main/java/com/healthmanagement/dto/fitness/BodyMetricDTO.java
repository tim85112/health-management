package com.healthmanagement.dto.fitness;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

import io.swagger.v3.oas.annotations.media.Schema;
import com.healthmanagement.model.fitness.BodyMetric;
import com.healthmanagement.model.member.*;

@Builder
@Data
@AllArgsConstructor
@NoArgsConstructor
public class BodyMetricDTO {

	private Integer id;

	@Schema(example = "1")
	private Integer userId;

	@Schema(example = "70.5")
	private Double weight;

	@Schema(example = "18.5")
	private Double bodyFat;

	@Schema(example = "32.0")
	private Double muscleMass;

	@Schema(example = "80.0")
	private Double waistCircumference;

	@Schema(example = "95.0")
	private Double hipCircumference;

	@Schema(example = "175.0")
	private Double height;

	@Schema(example = "22.5")
	private Double bmi;

	@Schema(example = "2025-04-08")
	private LocalDate dateRecorded;

	@Schema(example = "用戶姓名")
	private String userName;
	

	public static BodyMetricDTO fromEntity(BodyMetric bodyMetric, User user) {
		return BodyMetricDTO.builder().id(bodyMetric.getId()) 
				.userId(bodyMetric.getUserId()).userName(user != null ? user.getName() : null)
				.weight(bodyMetric.getWeight()).bodyFat(bodyMetric.getBodyFat()).height(bodyMetric.getHeight())
				.waistCircumference(bodyMetric.getWaistCircumference())
				.hipCircumference(bodyMetric.getHipCircumference()).dateRecorded(bodyMetric.getDateRecorded())
				.bmi(bodyMetric.getBmi()).muscleMass(bodyMetric.getMuscleMass()).build();
	}

	public static BodyMetricDTO fromEntity(BodyMetric bodyMetric) {
		return BodyMetricDTO.builder().id(bodyMetric.getId()).userId(bodyMetric.getUserId())
				.weight(bodyMetric.getWeight()).bodyFat(bodyMetric.getBodyFat()).height(bodyMetric.getHeight())
				.waistCircumference(bodyMetric.getWaistCircumference())
				.hipCircumference(bodyMetric.getHipCircumference()).dateRecorded(bodyMetric.getDateRecorded())
				.bmi(bodyMetric.getBmi()).muscleMass(bodyMetric.getMuscleMass()).build();
	}
}

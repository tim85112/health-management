package com.healthmanagement.dto.fitness;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

import io.swagger.v3.oas.annotations.media.Schema;
import com.healthmanagement.model.fitness.BodyMetric;

@Builder   
@Data
@AllArgsConstructor
@NoArgsConstructor
public class BodyMetricDTO {

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

    @Schema(example = "2025-04-08T12:30:00")
    private LocalDateTime dateRecorded;

    public static BodyMetricDTO fromEntity(BodyMetric bodyMetric) {
        return BodyMetricDTO.builder()
                .userId(bodyMetric.getUserId())
                .weight(bodyMetric.getWeight())
                .bodyFat(bodyMetric.getBodyFat())
                .height(bodyMetric.getHeight())
                .waistCircumference(bodyMetric.getWaistCircumference())
                .hipCircumference(bodyMetric.getHipCircumference())
                .dateRecorded(bodyMetric.getDateRecorded())
                .bmi(bodyMetric.getBmi())
                .build();
    }
}

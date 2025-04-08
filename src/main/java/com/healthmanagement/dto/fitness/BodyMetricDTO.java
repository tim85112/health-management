package com.healthmanagement.dto.fitness;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

import com.healthmanagement.model.fitness.BodyMetric;

@Builder   
@Data
@AllArgsConstructor
@NoArgsConstructor
public class BodyMetricDTO {

    private Integer id;                           
    private Integer userId;                       
    private Double weight;                       
    private Double bodyFat;                       
    private Double muscleMass;                    
    private Double waistCircumference;            
    private Double hipCircumference;              
    private Double height;                       
    private Double bmi;                           
    private LocalDateTime dateRecorded;           

    
 // 在 DTO 層中提供靜態方法
    public static BodyMetricDTO fromEntity(BodyMetric bodyMetric) {
        return BodyMetricDTO.builder()
                .userId(bodyMetric.getUserId())
                .weight(bodyMetric.getWeight())
                .bodyFat(bodyMetric.getBodyFat())
                .height(bodyMetric.getHeight())
                .waistCircumference(bodyMetric.getWaistCircumference())
                .hipCircumference(bodyMetric.getHipCircumference())
                .bmi(bodyMetric.getBmi())
                .build();
    }

}

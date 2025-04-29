package com.healthmanagement.dto.fitness;

import lombok.Data;

@Data
public class FitnessProgressUpdateDTO {
    private Integer goalId;
    private Double progressValue;
}
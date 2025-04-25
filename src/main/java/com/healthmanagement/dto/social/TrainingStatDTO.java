package com.healthmanagement.dto.social;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class TrainingStatDTO {
    private int accepted;
    private int rejected;
    private int pending;
}

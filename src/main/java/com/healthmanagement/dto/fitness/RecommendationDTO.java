package com.healthmanagement.dto.fitness;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class RecommendationDTO {
    private String type; // 建議類型 (例如：訓練建議, 營養建議, 目標設定建議)
    private String message; // 建議內容
}
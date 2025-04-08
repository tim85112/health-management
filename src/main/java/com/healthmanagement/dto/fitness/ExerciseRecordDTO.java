package com.healthmanagement.dto.fitness;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ExerciseRecordDTO {
    private Integer recordId;            // 運動紀錄 ID
    private Integer userId;              // 用戶 ID
    private String exerciseType;         // 運動類型
    private Integer exerciseDuration;    // 運動時長 (分鐘)
    private Double caloriesBurned;   // 消耗的卡路里
    private LocalDate exerciseDate;      // 運動日期

}

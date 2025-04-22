package com.healthmanagement.dto.fitness;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import com.healthmanagement.model.fitness.ExerciseRecord;  

import java.time.LocalDate;

import io.swagger.v3.oas.annotations.media.Schema;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ExerciseRecordDTO {

    private Integer recordId;

    @Schema(example = "1")
    private Integer userId;

    @Schema(example = "跑步")
    private String exerciseType;

    @Schema(example = "30")
    private Integer exerciseDuration;

    @Schema(example = "350.0")
    private Double caloriesBurned;

    @Schema(example = "2025-04-08")
    private LocalDate exerciseDate;
    
    @Schema(example = "'John Doe'") // 新增 userName 欄位
    private String userName;

    
    
    public ExerciseRecordDTO(ExerciseRecord exerciseRecord, String userName) {
        this.recordId = exerciseRecord.getRecordId();
        this.userId = exerciseRecord.getUserId();
        this.exerciseType = exerciseRecord.getExerciseType();
        this.exerciseDuration = exerciseRecord.getExerciseDuration();
        this.caloriesBurned = exerciseRecord.getCaloriesBurned();
        this.exerciseDate = exerciseRecord.getExerciseDate();
        this.userName = userName; // 直接賦值使用者姓名
    }

}

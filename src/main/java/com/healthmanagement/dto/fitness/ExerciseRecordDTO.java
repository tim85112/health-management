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

    @Schema(example = "123")
    private Integer userId;

    @Schema(example = "'Running'")
    private String exerciseType;

    @Schema(example = "30")
    private Integer exerciseDuration;

    @Schema(example = "350.0")
    private Double caloriesBurned;

    @Schema(example = "2025-04-08")
    private LocalDate exerciseDate;
    
    
    public ExerciseRecordDTO(ExerciseRecord exerciseRecord) {
        this.recordId = exerciseRecord.getRecordId();
        this.userId = exerciseRecord.getUserId();
        this.exerciseType = exerciseRecord.getExerciseType();
        this.exerciseDuration = exerciseRecord.getExerciseDuration();
        this.caloriesBurned = exerciseRecord.getCaloriesBurned();
        this.exerciseDate = exerciseRecord.getExerciseDate();
    }

}

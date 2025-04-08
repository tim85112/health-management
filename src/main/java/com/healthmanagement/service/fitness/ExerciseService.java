package com.healthmanagement.service.fitness;

import java.util.List;

import com.healthmanagement.dto.fitness.ExerciseRecordDTO;

public interface ExerciseService {
    ExerciseRecordDTO saveExerciseRecord(ExerciseRecordDTO exerciseRecordDTO);
    void deleteExerciseRecord(Integer recordId);
    List<ExerciseRecordDTO> getExerciseRecordsByUserId(Integer userId);

}

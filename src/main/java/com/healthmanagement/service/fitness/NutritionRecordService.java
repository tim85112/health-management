package com.healthmanagement.service.fitness;

import com.healthmanagement.dto.fitness.NutritionRecordDTO;

import java.time.LocalDateTime;
import java.util.List;
import com.healthmanagement.dto.fitness.NutritionSummaryDTO;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface NutritionRecordService {
    NutritionRecordDTO addNutritionRecord(NutritionRecordDTO recordDTO);
    NutritionRecordDTO getNutritionRecordById(Integer recordId);
    List<NutritionRecordDTO> getAllNutritionRecords();
    List<NutritionRecordDTO> getNutritionRecordsByUserId(Integer userId);
    List<NutritionRecordDTO> getNutritionRecordsByUserAndDateRange(Integer userId, LocalDateTime startDate, LocalDateTime endDate);
    NutritionRecordDTO updateNutritionRecord(Integer recordId, NutritionRecordDTO recordDTO);
    void deleteNutritionRecord(Integer recordId);
    Page<NutritionRecordDTO> searchNutritionRecords(Integer userId, String name, LocalDateTime startDate, LocalDateTime endDate, String mealtime, Pageable pageable);
    NutritionSummaryDTO getNutritionSummary(Integer userId, LocalDateTime startDate, LocalDateTime endDate);
	 List<NutritionRecordDTO> findByUserId(Integer userId);
	 List<NutritionRecordDTO> findByUserIdAndRecordDateBetween(Integer userId, LocalDateTime startDate, LocalDateTime endDate);
    }      
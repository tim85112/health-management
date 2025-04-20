package com.healthmanagement.controller.fitness;

import com.healthmanagement.dto.fitness.NutritionRecordDTO;
import com.healthmanagement.service.fitness.NutritionRecordService;
import lombok.RequiredArgsConstructor;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/tracking/nutrition")
@RequiredArgsConstructor
public class NutritionRecordController {

    private final NutritionRecordService nutritionRecordService;

    @PostMapping("/add")
    public ResponseEntity<?> addNutritionRecord(@RequestBody NutritionRecordDTO nutritionRecordDTO) {
        System.out.println("後端接收到的 mealtime (新增): " + nutritionRecordDTO.getMealtime()); // 添加這行
        try {
            nutritionRecordService.addNutritionRecord(nutritionRecordDTO);
            return ResponseEntity.ok("飲食記錄新增成功");
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @GetMapping("/{recordId}")
    public ResponseEntity<NutritionRecordDTO> getNutritionRecordById(@PathVariable Integer recordId) {
        NutritionRecordDTO record = nutritionRecordService.getNutritionRecordById(recordId);
        return ResponseEntity.ok(record);
    }

    @GetMapping("/search")
    public ResponseEntity<Page<NutritionRecordDTO>> searchNutritionRecords(
            @RequestParam(required = false) Integer userId,
            @RequestParam(required = false) String name,
            @RequestParam(required = false) LocalDateTime startDate,
            @RequestParam(required = false) LocalDateTime endDate,
            @RequestParam(required = false) String mealtime,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {

        Page<NutritionRecordDTO> records = nutritionRecordService.searchNutritionRecords(userId, name, startDate, endDate, mealtime, PageRequest.of(page, size));
        return ResponseEntity.ok(records);
    }

    @GetMapping("/user/{userId}")
    public ResponseEntity<List<NutritionRecordDTO>> getNutritionRecordsByUserId(@PathVariable Integer userId) {
        List<NutritionRecordDTO> records = nutritionRecordService.getNutritionRecordsByUserId(userId);
        return ResponseEntity.ok(records);
    }

    @GetMapping("/user/{userId}/date-range")
    public ResponseEntity<List<NutritionRecordDTO>> getNutritionRecordsByUserAndDateRange(
            @PathVariable Integer userId,
            @RequestParam LocalDateTime startDate,
            @RequestParam LocalDateTime endDate) {
        List<NutritionRecordDTO> records = nutritionRecordService.getNutritionRecordsByUserAndDateRange(userId, startDate, endDate);
        return ResponseEntity.ok(records);
    }

    @PutMapping("/{recordId}")
    public ResponseEntity<NutritionRecordDTO> updateNutritionRecord(@PathVariable Integer recordId, @RequestBody NutritionRecordDTO recordDTO) {
        NutritionRecordDTO updatedRecord = nutritionRecordService.updateNutritionRecord(recordId, recordDTO);
        return ResponseEntity.ok(updatedRecord);
    }

    @DeleteMapping("/{recordId}")
    public ResponseEntity<Void> deleteNutritionRecord(@PathVariable Integer recordId) {
        nutritionRecordService.deleteNutritionRecord(recordId);
        return new ResponseEntity<>(HttpStatus.NO_CONTENT);
    }
}
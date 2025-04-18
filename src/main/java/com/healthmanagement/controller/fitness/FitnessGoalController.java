package com.healthmanagement.controller.fitness;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.healthmanagement.dto.fitness.FitnessGoalDTO;
import com.healthmanagement.service.fitness.FitnessGoalService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

import java.util.List;

@RestController
@RequestMapping("/api/tracking/fitnessgoals")
@RequiredArgsConstructor
@CrossOrigin(origins = "http://localhost:5174")
@Tag(name = "Fitness Tracking", description = "健身追蹤管理 API")
public class FitnessGoalController {

    private final FitnessGoalService fitnessGoalService;

    @Operation(summary = "建立健身目標", description = "建立新的健身目標資料")
    @PostMapping
    public ResponseEntity<FitnessGoalDTO> createFitnessGoal(
            @RequestBody FitnessGoalDTO fitnessGoalDTO) {
        FitnessGoalDTO createdGoal = fitnessGoalService.createFitnessGoal(fitnessGoalDTO);
        return ResponseEntity.status(HttpStatus.CREATED).body(createdGoal);
    }

    @Operation(summary = "更新健身目標", description = "根據目標 ID 更新指定的健身目標資料")
    @PutMapping("/{goalId}")
    public ResponseEntity<FitnessGoalDTO> updateFitnessGoal(
            @Parameter(description = "健身目標 ID") @PathVariable Integer goalId,
            @RequestBody FitnessGoalDTO fitnessGoalDTO) {
        FitnessGoalDTO updatedGoal = fitnessGoalService.updateFitnessGoal(goalId, fitnessGoalDTO);
        return ResponseEntity.ok(updatedGoal);
    }

    @Operation(summary = "刪除健身目標", description = "根據目標 ID 刪除健身目標資料")
    @DeleteMapping("/{goalId}")
    public ResponseEntity<Void> deleteFitnessGoal(
            @Parameter(description = "健身目標 ID") @PathVariable Integer goalId) {
        fitnessGoalService.deleteFitnessGoal(goalId);
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "取得用戶的所有健身目標", description = "根據用戶 ID 取得所有健身目標資料")
    @GetMapping("/user/{userId}")
    public ResponseEntity<List<FitnessGoalDTO>> getAllFitnessGoalsByUserId(
            @Parameter(description = "用戶 ID") @PathVariable Integer userId) {
        List<FitnessGoalDTO> fitnessGoals = fitnessGoalService.getAllFitnessGoalsByUserId(userId);
        return ResponseEntity.ok(fitnessGoals);
    }
    
    @Operation(summary = "根據姓名取得健身目標", description = "根據用戶姓名取得所有健身目標資料")
    @GetMapping("/user/by-name")
    public ResponseEntity<List<FitnessGoalDTO>> getAllFitnessGoalsByUserName(
            @Parameter(description = "用戶姓名") @RequestParam String name) {
        List<FitnessGoalDTO> fitnessGoals = fitnessGoalService.getAllFitnessGoalsByUserName(name);
        return ResponseEntity.ok(fitnessGoals);
    }
    
    @Operation(summary = "根據日期範圍取得健身目標", description = "根據開始日期和結束日期取得所有健身目標資料")
    @GetMapping("/by-date-range")
    public ResponseEntity<List<FitnessGoalDTO>> getAllFitnessGoalsByDateRange(
            @Parameter(description = "開始日期 (YYYY-MM-DD)") @RequestParam(required = false) String startDate,
            @Parameter(description = "結束日期 (YYYY-MM-DD)") @RequestParam(required = false) String endDate) {
        List<FitnessGoalDTO> fitnessGoals = fitnessGoalService.getAllFitnessGoalsByDateRange(startDate, endDate);
        return ResponseEntity.ok(fitnessGoals);
    }
    @Operation(summary = "根據用戶 ID 和日期範圍取得健身目標", description = "根據用戶 ID、開始日期和結束日期取得所有健身目標資料")
    @GetMapping("/user/{userId}/by-date-range")
    public ResponseEntity<List<FitnessGoalDTO>> getAllFitnessGoalsByUserIdAndDateRange(
            @Parameter(description = "用戶 ID") @PathVariable Integer userId,
            @Parameter(description = "開始日期 (YYYY-MM-DD)") @RequestParam(required = false) String startDate,
            @Parameter(description = "結束日期 (YYYY-MM-DD)") @RequestParam(required = false) String endDate) {
        List<FitnessGoalDTO> fitnessGoals = fitnessGoalService.getAllFitnessGoalsByUserIdAndDateRange(userId, startDate, endDate);
        return ResponseEntity.ok(fitnessGoals);
    }
}

package com.healthmanagement.controller.fitness;

import com.healthmanagement.dto.fitness.FitnessGoalDTO;
import com.healthmanagement.dto.fitness.FitnessProgressUpdateDTO;
import com.healthmanagement.service.fitness.FitnessGoalService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/tracking/fitnessgoals")
@RequiredArgsConstructor
@Tag(name = "Fitness Tracking", description = "健身追蹤管理 API")
public class FitnessGoalController {

    private final FitnessGoalService fitnessGoalService;

    @PostMapping
    @Operation(summary = "建立健身目標")
    public ResponseEntity<FitnessGoalDTO> createFitnessGoal(@RequestBody FitnessGoalDTO fitnessGoalDTO) {
        FitnessGoalDTO createdGoal = fitnessGoalService.createFitnessGoal(fitnessGoalDTO);
        return ResponseEntity.status(HttpStatus.CREATED).body(createdGoal);
    }

    @PutMapping("/{goalId}")
    @Operation(summary = "更新健身目標")
    public ResponseEntity<FitnessGoalDTO> updateFitnessGoal(@PathVariable Integer goalId, @RequestBody FitnessGoalDTO fitnessGoalDTO) {
        FitnessGoalDTO updatedGoal = fitnessGoalService.updateFitnessGoal(goalId, fitnessGoalDTO);
        return ResponseEntity.ok(updatedGoal);
    }

    @DeleteMapping("/{goalId}")
    @Operation(summary = "刪除健身目標")
    public ResponseEntity<Void> deleteFitnessGoal(@PathVariable Integer goalId) {
        fitnessGoalService.deleteFitnessGoal(goalId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping
    @Operation(summary = "根據條件查詢健身目標 (分頁)")
    public ResponseEntity<Page<FitnessGoalDTO>> getAllFitnessGoalsByCriteria(
            @RequestParam(required = false) @Parameter(description = "用戶 ID") Integer userId,
            @RequestParam(required = false) @Parameter(description = "用戶姓名 (模糊查詢)") String name,
            @RequestParam(required = false) @Parameter(description = "開始日期 (YYYY-MM-DD)") String startDate,
            @RequestParam(required = false) @Parameter(description = "結束日期 (YYYY-MM-DD)") String endDate,
            @RequestParam(required = false) @Parameter(description = "目標類型") String goalType,
            @RequestParam(required = false) @Parameter(description = "狀態") String status,
            @PageableDefault(page = 0, size = 10) Pageable pageable) {
        Page<FitnessGoalDTO> fitnessGoalsPage = fitnessGoalService.getAllFitnessGoalsByCriteria(userId, name, startDate, endDate, goalType, status, pageable);
        return ResponseEntity.ok(fitnessGoalsPage);
    }

    @GetMapping("/{goalId}")
    @Operation(summary = "根據 ID 取得健身目標")
    public ResponseEntity<FitnessGoalDTO> getFitnessGoalById(@PathVariable Integer goalId) {
        FitnessGoalDTO fitnessGoal = fitnessGoalService.getFitnessGoalById(goalId);
        return ResponseEntity.ok(fitnessGoal);
    }
    @PostMapping("/progress")
    @Operation(summary = "更新健身目標的目前進度")
    public ResponseEntity<FitnessGoalDTO> updateGoalProgress(@RequestBody FitnessProgressUpdateDTO progressUpdateDTO) {
        FitnessGoalDTO updatedGoal = fitnessGoalService.updateGoalProgress(
                progressUpdateDTO.getGoalId(),
                progressUpdateDTO.getProgressValue()
        );
        return ResponseEntity.ok(updatedGoal);
    }
}
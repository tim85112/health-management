package com.healthmanagement.controller.fitness;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.healthmanagement.dto.fitness.ExerciseRecordDTO;
import com.healthmanagement.service.fitness.ExerciseService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/tracking/exercise-records")
@RequiredArgsConstructor
@CrossOrigin(origins = "http://localhost:5174")
@Tag(name = "Fitness Tracking", description = "健身追蹤管理 API")
public class ExerciseRecordsController {
    
    @Autowired
    private ExerciseService exerciseService;

    @Operation(summary = "新增運動紀錄", description = "創建一條新的運動紀錄")
    @PostMapping
    public ResponseEntity<ExerciseRecordDTO> saveExerciseRecord(@RequestBody ExerciseRecordDTO exerciseRecordDTO) {
        return ResponseEntity.ok(exerciseService.saveExerciseRecord(exerciseRecordDTO));
    }

    @Operation(summary = "刪除運動紀錄", description = "根據紀錄 ID 刪除一條運動紀錄")
    @DeleteMapping("/{recordId}")
    public ResponseEntity<Void> deleteExerciseRecord(@Parameter(description = "運動紀錄 ID") @PathVariable Integer recordId) {
        exerciseService.deleteExerciseRecord(recordId);
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "根據用戶 ID 查詢所有運動紀錄", description = "根據用戶 ID 查詢該用戶的所有運動紀錄")
    @GetMapping("/user/{userId}")
    public ResponseEntity<List<ExerciseRecordDTO>> getExerciseRecordsByUserId(@Parameter(description = "用戶 ID") @PathVariable Integer userId) {
        return ResponseEntity.ok(exerciseService.getExerciseRecordsByUserId(userId));
    }
    @Operation(summary = "更新運動紀錄", description = "根據運動紀錄 ID 更新運動紀錄")
    @PutMapping("/{recordId}")
    public ResponseEntity<ExerciseRecordDTO> updateExerciseRecord(
            @Parameter(description = "運動紀錄 ID") @PathVariable Integer recordId,
            @RequestBody ExerciseRecordDTO exerciseRecordDTO) {
        ExerciseRecordDTO updatedRecord = exerciseService.updateExerciseRecord(recordId, exerciseRecordDTO);
        if (updatedRecord != null) {
            return ResponseEntity.ok(updatedRecord); // 返回更新後的紀錄
        }
        return ResponseEntity.notFound().build(); // 如果找不到紀錄，返回 404
    }
    @Operation(summary = "根據用戶 ID 和姓名模糊查詢運動紀錄", description = "根據用戶 ID 和包含特定姓名的用戶查詢運動紀錄")
    @GetMapping("/user/{userId}/by-name")
    public ResponseEntity<List<ExerciseRecordDTO>> getExerciseRecordsByUserIdAndUserName(
            @Parameter(description = "用戶 ID") @PathVariable Integer userId,
            @Parameter(description = "用戶姓名關鍵字") @RequestParam String userName) {
        return ResponseEntity.ok(exerciseService.getExerciseRecordsByUserIdAndUserName(userId, userName));
    }

    @Operation(summary = "根據姓名模糊查詢所有運動紀錄", description = "查詢包含特定姓名的用戶的所有運動紀錄")
    @GetMapping("/by-name")
    public ResponseEntity<List<ExerciseRecordDTO>> getExerciseRecordsByUserName(
            @Parameter(description = "用戶姓名關鍵字") @RequestParam String userName) {
        return ResponseEntity.ok(exerciseService.getExerciseRecordsByUserName(userName));
    }

}

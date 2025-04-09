package com.healthmanagement.controller.fitness;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.bind.annotation.CrossOrigin;

import com.healthmanagement.dto.fitness.AchievementDTO;
import com.healthmanagement.service.fitness.AchievementService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/tracking/achievements")
@RequiredArgsConstructor
@Tag(name = "Fitness Tracking", description = "健身追蹤管理 API")
@CrossOrigin(origins = "http://localhost:5174")
public class AchievementController {

    private final AchievementService achievementService;

    @Operation(summary = "創建獎勳", description = "根據給定的獎勳資料創建一個新的獎勳")
    @PostMapping
    public ResponseEntity<AchievementDTO> createAchievement(@RequestBody AchievementDTO achievementDTO) {
        AchievementDTO createdAchievement = achievementService.createAchievement(achievementDTO);
        return ResponseEntity.status(HttpStatus.CREATED).body(createdAchievement);
    }

    @Operation(summary = "更新獎勳", description = "根據 ID 更新指定的獎勳資料")
    @PutMapping("/{achievementId}")
    public ResponseEntity<AchievementDTO> updateAchievement(
            @Parameter(description = "獎勳 ID") @PathVariable Integer achievementId,
            @RequestBody AchievementDTO achievementDTO) {
        AchievementDTO updatedAchievement = achievementService.updateAchievement(achievementId, achievementDTO);
        return ResponseEntity.ok(updatedAchievement);
    }

    @Operation(summary = "刪除獎勳", description = "根據獎勳 ID 刪除指定的獎勳")
    @DeleteMapping("/{achievementId}")
    public ResponseEntity<Void> deleteAchievement(@Parameter(description = "獎勳 ID") @PathVariable Integer achievementId) {
        achievementService.deleteAchievement(achievementId);
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "查詢單個獎勳", description = "根據獎勳 ID 查詢一條獎勳記錄")
    @GetMapping("/{achievementId}")
    public ResponseEntity<AchievementDTO> getAchievementById(@Parameter(description = "獎勳 ID") @PathVariable Integer achievementId) {
        AchievementDTO achievement = achievementService.getAchievementById(achievementId);
        return ResponseEntity.ok(achievement);
    }

    @Operation(summary = "查詢用戶的所有獎勳", description = "根據用戶 ID 查詢該用戶的所有獎勳")
    @GetMapping("/user/{userId}")
    public ResponseEntity<List<AchievementDTO>> getAllAchievementsByUserId(@Parameter(description = "用戶 ID") @PathVariable Integer userId) {
        List<AchievementDTO> achievements = achievementService.getAllAchievementsByUserId(userId);
        return ResponseEntity.ok(achievements);
    }
}

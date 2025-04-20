package com.healthmanagement.controller.fitness;

import com.healthmanagement.dto.fitness.AchievementDTO;
import com.healthmanagement.service.fitness.AchievementService;
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
@RequestMapping("/api/tracking/achievements") 
@RequiredArgsConstructor
@Tag(name = "Admin - Fitness Tracking", description = "Admin 健身追蹤管理 API")
public class AchievementController {

    private final AchievementService achievementService;

    @GetMapping("/all")
    @Operation(summary = "獲取所有獎章 (後台 - 分頁)")
    public ResponseEntity<Page<AchievementDTO>> getAllAchievements(
            @PageableDefault(page = 0, size = 10) Pageable pageable) {
        Page<AchievementDTO> achievements = achievementService.getAllAchievements(pageable);
        return new ResponseEntity<>(achievements, HttpStatus.OK);
    }

    @GetMapping("/search")
    @Operation(summary = "根據條件查詢獎章 (後台 - 分頁)")
    public ResponseEntity<Page<AchievementDTO>> searchAchievements(
            @RequestParam(required = false) @Parameter(description = "用戶ID") Integer userId,
            @RequestParam(required = false) @Parameter(description = "用戶姓名 (模糊查詢)") String name,
            @RequestParam(required = false) @Parameter(description = "獎章類型") String achievementType,
            @RequestParam(required = false) @Parameter(description = "獎章標題 (模糊查詢)") String title,
            @RequestParam(required = false) @Parameter(description = "起始日期 (YYYY-MM-DD)") String startDate,
            @RequestParam(required = false) @Parameter(description = "結束日期 (YYYY-MM-DD)") String endDate,
            @PageableDefault(page = 0, size = 10) Pageable pageable) {
        Page<AchievementDTO> achievements = achievementService.searchAchievements(userId, name, achievementType, title, startDate, endDate, pageable);
        return new ResponseEntity<>(achievements, HttpStatus.OK);
    }

    @GetMapping("/{achievementId}")
    @Operation(summary = "根據ID獲取獎章 (後台)")
    public ResponseEntity<AchievementDTO> getAchievementById(@PathVariable @Parameter(description = "獎章ID") Integer achievementId) {
        AchievementDTO achievement = achievementService.getAchievementById(achievementId);
        return ResponseEntity.of(java.util.Optional.ofNullable(achievement));
    }

    @PostMapping("/{userId}")
    @Operation(summary = "新增獎章 (後台)")
    public ResponseEntity<AchievementDTO> addAchievementByAdmin(
            @PathVariable @Parameter(description = "用戶ID") Integer userId,
            @RequestParam @Parameter(description = "獎章類型") String achievementType,
            @RequestParam @Parameter(description = "獎章標題") String title,
            @RequestParam(required = false) @Parameter(description = "獎章描述") String description) {
        AchievementDTO newAchievement = achievementService.addAchievement(userId, achievementType, title, description);
        return new ResponseEntity<>(newAchievement, HttpStatus.CREATED);
    }

    @DeleteMapping("/{achievementId}")
    @Operation(summary = "刪除獎章 (後台)")
    public ResponseEntity<Void> deleteAchievement(@PathVariable @Parameter(description = "獎章ID") Integer achievementId) {
        achievementService.deleteAchievement(achievementId);
        return new ResponseEntity<>(HttpStatus.NO_CONTENT);
    }

    // 移除內部 API 路徑，getAllAchievements 已經處理了所有獎章的獲取
    // @GetMapping("/search") // 前台獲取用戶獎章的 API 保持不變
}
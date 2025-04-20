package com.healthmanagement.controller.fitness;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.healthmanagement.dto.fitness.BodyMetricDTO;
import com.healthmanagement.service.fitness.BodyMetricService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/tracking/body-metrics")
@RequiredArgsConstructor
@Tag(name = "Fitness Tracking", description = "健身追蹤管理 API")
public class BodyMetricController {

    private final BodyMetricService bodyMetricService;

    @Operation(summary = "創建身體數據", description = "根據用戶的身體數據創建一條新記錄")
    @PostMapping
    public ResponseEntity<BodyMetricDTO> createBodyMetric(@RequestBody BodyMetricDTO bodyMetricDTO) {
        return ResponseEntity.ok(bodyMetricService.saveBodyMetrics(bodyMetricDTO));
    }

    @Operation(summary = "更新身體數據", description = "根據 ID 更新用戶的身體數據")
    @PutMapping("/{id}")
    public ResponseEntity<BodyMetricDTO> updateBodyMetric(
            @Parameter(description = "身體數據 ID") @PathVariable Integer id,
            @RequestBody BodyMetricDTO bodyMetricDTO) {
        return ResponseEntity.ok(bodyMetricService.updateBodyMetric(id, bodyMetricDTO));
    }

    @Operation(summary = "刪除身體數據", description = "根據 ID 刪除用戶的身體數據記錄")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteBodyMetric(@Parameter(description = "身體數據 ID") @PathVariable Integer id) {
        bodyMetricService.deleteBodyMetric(id);
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "查詢用戶的所有身體數據", description = "根據用戶 ID 查詢所有身體數據")
    @GetMapping("/user/{userId}")
    public ResponseEntity<List<BodyMetricDTO>> getBodyMetricsByUserId(@Parameter(description = "用戶 ID") @PathVariable Integer userId) {
        return ResponseEntity.ok(bodyMetricService.findByUserId(userId));
    }

    @Operation(summary = "多條件查詢身體數據 (分頁)", description = "根據用戶 ID、姓名和日期範圍查詢身體數據，支援分頁")
    @GetMapping("/search")
    public ResponseEntity<Page<BodyMetricDTO>> findBodyMetricsByCriteriaWithPagination(
            @Parameter(description = "用戶 ID") @RequestParam(value = "userId", required = false) Integer userId,
            @Parameter(description = "用戶姓名 (模糊查詢)") @RequestParam(value = "name", required = false) String name,
            @Parameter(description = "開始日期 (YYYY-MM-DD)") @RequestParam(value = "startDate", required = false) String startDate,
            @Parameter(description = "結束日期 (YYYY-MM-DD)") @RequestParam(value = "endDate", required = false) String endDate,
            @Parameter(description = "頁碼 (從 0 開始)") @RequestParam(value = "page", defaultValue = "0") int page,
            @Parameter(description = "每頁大小") @RequestParam(value = "size", defaultValue = "10") int size) {

        Pageable pageable = PageRequest.of(page, size);
        Page<BodyMetricDTO> bodyMetricPage = bodyMetricService.findByMultipleCriteriaWithPagination(userId, name, startDate, endDate, pageable);
        return ResponseEntity.ok(bodyMetricPage);
    }
}
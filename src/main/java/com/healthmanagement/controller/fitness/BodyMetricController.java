package com.healthmanagement.controller.fitness;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.healthmanagement.dto.fitness.BodyMetricDTO;
import com.healthmanagement.service.fitness.BodyMetricService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/tracking/body-metrics")
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
}

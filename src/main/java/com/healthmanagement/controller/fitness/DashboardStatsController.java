package com.healthmanagement.controller.fitness;

import com.healthmanagement.dto.fitness.DashboardStatsDTO;
import com.healthmanagement.service.fitness.DashboardStatsService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/fitness/dashboard")
@RequiredArgsConstructor
public class DashboardStatsController {

    private final DashboardStatsService dashboardStatsService;

    @GetMapping("/stats")
    public ResponseEntity<DashboardStatsDTO> getDashboardStats() {
        DashboardStatsDTO stats = dashboardStatsService.getDashboardStats();
        return ResponseEntity.ok(stats);
    }
}
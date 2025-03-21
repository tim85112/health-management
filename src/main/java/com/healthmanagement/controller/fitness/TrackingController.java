package com.healthmanagement.controller.fitness;

import com.healthmanagement.util.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/tracking")
@Tag(name = "Fitness Tracking", description = "Fitness tracking management APIs")
public class TrackingController {

    @GetMapping
    @Operation(summary = "Get all tracking records", description = "Retrieve a list of all fitness tracking records")
    public ResponseEntity<ApiResponse<String>> getAllTrackingRecords() {
        // 此方法将由组员实现
        return ResponseEntity.ok(ApiResponse.success("Tracking records will be implemented by Team Member C"));
    }
}
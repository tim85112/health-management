package com.healthmanagement.controller.course;

import com.healthmanagement.util.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/courses")
@Tag(name = "Courses", description = "Course management APIs")
public class CourseController {

    @GetMapping
    @Operation(summary = "Get all courses", description = "Retrieve a list of all courses")
    public ResponseEntity<ApiResponse<String>> getAllCourses() {
        // 此方法将由组员实现
        return ResponseEntity.ok(ApiResponse.success("Course list will be implemented by Team Member B"));
    }
}
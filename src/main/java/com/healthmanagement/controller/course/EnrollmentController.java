package com.healthmanagement.controller.course; // 確保這是正確的 package

import com.healthmanagement.dto.course.EnrollmentDTO;
import com.healthmanagement.service.course.EnrollmentService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
@RestController
@RequestMapping("/api/enrollments")
@CrossOrigin(origins = "*") // // 允許 Vue 前端的跨域請求
@PreAuthorize("isAuthenticated()") // 需要登入才能存取
@Tag(name = "報名管理", description = "報名管理API")
public class EnrollmentController {

    @Autowired
    private EnrollmentService enrollmentService;

    @Operation(summary = "報名課程")
    @PreAuthorize("hasRole('user')") // 只有 user 可以報名
    @PostMapping("/users/{userId}/courses/{courseId}")
    public ResponseEntity<?> enrollUserToCourse(@PathVariable Integer userId, @PathVariable Integer courseId) {
        try {
            EnrollmentDTO enrollmentDTO = enrollmentService.enrollUserToCourse(userId, courseId);
            return new ResponseEntity<>(enrollmentDTO, HttpStatus.CREATED);
        } catch (IllegalStateException e) {
            return ResponseEntity.badRequest().body(e.getMessage()); // 回傳 ResponseEntity<String>
        }
    }

    @Operation(summary = "取消報名課程")
    @PreAuthorize("hasAuthority('admin', 'coach') or @userSecurity.isCurrentUser(#userId)")
    // 只有 admin, coach, 跟特定user 可以取消
    @DeleteMapping("/{enrollmentId}")
    public ResponseEntity<Void> cancelEnrollment(@PathVariable Integer enrollmentId) {
        enrollmentService.cancelEnrollment(enrollmentId);
        return ResponseEntity.noContent().build();
    }
    
    @Operation(summary = "依照報名ID查詢報名資訊")
    @PreAuthorize("hasAuthority('admin', 'coach') or @userSecurity.isCurrentUser(#userId)")
    // 只有 admin, coach, 跟特定user 可以查詢
    @GetMapping("/{enrollmentId}")
    public ResponseEntity<EnrollmentDTO> getEnrollmentById(@PathVariable Integer enrollmentId) {
        EnrollmentDTO enrollment = enrollmentService.getEnrollmentById(enrollmentId);
        if (enrollment != null) {
            return ResponseEntity.ok(enrollment);
        }
        return ResponseEntity.notFound().build();
    }

    @Operation(summary = "查詢特定使用者的所有報名紀錄")
    @PreAuthorize("hasAuthority('admin', 'coach') or @userSecurity.isCurrentUser(#userId)")
    // 只有 admin, coach, 跟特定user 可以查詢
    @GetMapping("/users/{userId}")
    public ResponseEntity<List<EnrollmentDTO>> getEnrollmentsByUserId(@PathVariable Integer userId) {
        return ResponseEntity.ok(enrollmentService.getEnrollmentsByUserId(userId));
    }

    @Operation(summary = "查詢特定課程的所有報名紀錄")
    @PreAuthorize("hasAuthority('admin', 'coach')") // 只有 admin, coach 可以查詢
    @GetMapping("/courses/{courseId}")
    public ResponseEntity<List<EnrollmentDTO>> getEnrollmentsByCourseId(@PathVariable Integer courseId) {
        return ResponseEntity.ok(enrollmentService.getEnrollmentsByCourseId(courseId));
    }

    @Operation(summary = "查詢特定課程是否已滿")
    @GetMapping("/courses/{courseId}/is-full")
    public ResponseEntity<Boolean> isCourseFull(@PathVariable Integer courseId) {
        return ResponseEntity.ok(enrollmentService.isCourseFull(courseId));
    }

    @Operation(summary = "查詢特定使用者是否已報名特定課程")
    @PreAuthorize("hasAuthority('admin', 'coach') or @userSecurity.isCurrentUser(#userId)")
    // 只有 admin, coach, 跟特定user 可以查詢
    @GetMapping("/users/{userId}/courses/{courseId}/is-enrolled")
    public ResponseEntity<Boolean> isUserEnrolled(@PathVariable Integer userId, @PathVariable Integer courseId) {
        return ResponseEntity.ok(enrollmentService.isUserEnrolled(userId, courseId));
    }

    @Operation(summary = "查詢特定課程的已報名人數")
    @GetMapping("/courses/{courseId}/count")
    public ResponseEntity<Integer> getEnrolledCount(@PathVariable Integer courseId) {
        return ResponseEntity.ok(enrollmentService.getEnrolledCount(courseId));
    }
}
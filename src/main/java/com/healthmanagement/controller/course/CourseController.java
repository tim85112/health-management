package com.healthmanagement.controller.course;

import com.healthmanagement.dto.course. CourseRequest;
import com.healthmanagement.dto.course. CourseResponse;
import com.healthmanagement.service.course.CourseService;
import com.healthmanagement.service.course.EnrollmentService; // 引入 EnrollmentService
import com.healthmanagement.service.member.UserService; // 引入 UserService 用於根據郵箱查找使用者

import com.healthmanagement.dto.course.CourseInfoDTO;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.annotation.AuthenticationPrincipal; // 引入 AuthenticationPrincipal
import org.springframework.security.core.userdetails.UserDetails; // 引入 UserDetails
import org.springframework.web.bind.annotation.*;
import org.springframework.dao.EmptyResultDataAccessException;
import jakarta.persistence.EntityNotFoundException;
import jakarta.validation.Valid;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Collections;
import java.util.Optional; // 引入 Optional

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@RestController
@RequestMapping("/api/courses")
@CrossOrigin(origins = "*") // 允許 Vue 前端的跨域請求
@Tag(name = "課程管理", description = "課程管理API")
public class CourseController {

	private static final Logger logger = LoggerFactory.getLogger(CourseController.class);

    private final CourseService courseService;
    private final EnrollmentService enrollmentService;
    private final UserService userService;

    @Autowired
    public CourseController(CourseService courseService, EnrollmentService enrollmentService, UserService userService) {
        this.courseService = courseService;
        this.enrollmentService = enrollmentService;
        this.userService = userService;
    }

	// 查詢所有課程，包含使用者的報名/預約狀態和人數
    @Operation(summary = "查詢所有課程及使用者狀態")
    @GetMapping
    public ResponseEntity<List<CourseInfoDTO>> getAllCoursesWithStatus(@AuthenticationPrincipal UserDetails userDetails) {
        logger.info("收到獲取所有課程（包含使用者狀態）的請求。");
        // 從 UserDetails 中獲取使用者 ID
        Integer userId = null;
        if (userDetails != null) {
            try {
                // 根據 UserDetails 中的信息（通常是郵箱或使用者名）查找 User 實體以獲取 ID
                String identifier = userDetails.getUsername(); // 假設 getUsername() 返回郵箱或使用者名
                Optional<com.healthmanagement.model.member.User> userOptional = userService.findByEmail(identifier); // 或 findByUsername

                if (userOptional.isPresent()) {
                     userId = userOptional.get().getId();

                     logger.info("已認證使用者 (Principal: {}) 查詢所有課程和狀態。", identifier);
                } else {
                     logger.warn("已認證使用者 (Principal: {}) 找不到對應 User 實體，無法查詢個人狀態。", identifier);
                     // userId 保持為 null
                }
            } catch (Exception e) {
                 logger.error("從 UserDetails 獲取使用者 ID 失敗。", e);
                 // userId 保持為 null
            }
        } else {
            logger.info("匿名使用者查詢所有課程和狀態 (不含個人狀態)。");
        }

        // 調用 EnrollmentService 中包含使用者狀態的方法
        List<CourseInfoDTO> courses = enrollmentService.getAllCoursesWithUserStatus(userId);
        logger.info("返回 {} 個課程（包含使用者狀態）。", courses.size());
        return ResponseEntity.ok(courses);
    }

	// 依照課程ID查詢。
    @Operation(summary = "依照課程ID查詢")
    @GetMapping("/{id}")
    public ResponseEntity< CourseResponse> getCourseById(@PathVariable Integer id) {
        logger.info("收到依課程 ID 查詢請求：{}", id);
        try {
             CourseResponse course = courseService.getCourseById(id);
            logger.info("返回課程 ID: {}。", id);
            return ResponseEntity.ok(course);
        } catch (EntityNotFoundException e) {
            logger.warn("找不到課程 ID: {}。", id, e);
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(null);
        } catch (Exception e) {
            logger.error("查詢課程 ID {} 時發生錯誤。", id, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null);
        }
    }

    // 新增課程 (需admin)
    @Operation(summary = "新增課程 (需admin)")
    @PreAuthorize("hasAuthority('admin')")
    @PostMapping
    public ResponseEntity< CourseResponse> createCourse(@Valid @RequestBody  CourseRequest courseRequest) {
        logger.info("收到創建課程請求：{}", courseRequest.getName());
        try {
             CourseResponse createdCourse = courseService.createCourse(courseRequest);
            logger.info("課程創建成功，ID: {}。", createdCourse.getId());
            return ResponseEntity.status(HttpStatus.CREATED).body(createdCourse);
        } catch (Exception e) {
              logger.error("創建課程 {} 時發生錯誤。", courseRequest.getName(), e);
              return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null);
         }
     }

    // 修改課程 (需admin)
    @Operation(summary = "修改課程 (需admin)")
    @PreAuthorize("hasAuthority('admin')")
    @PutMapping("/{id}")
    public ResponseEntity< CourseResponse> updateCourse(@PathVariable Integer id, @RequestBody  CourseRequest courseRequest) {
        logger.info("收到更新課程 ID 為 {} 的請求。", id);
        try {
             CourseResponse updatedCourse = courseService.updateCourse(id, courseRequest);
            logger.info("課程 ID {} 更新成功。", id);
            return ResponseEntity.ok(updatedCourse);
        } catch (EntityNotFoundException e) {
              logger.warn("嘗試更新課程 ID 為 {}，但未找到。", id, e);
              return ResponseEntity.status(HttpStatus.NOT_FOUND).body(null);
         } catch (Exception e) {
              logger.error("更新課程 ID {} 時發生錯誤。", id, e);
              return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null);
         }
     }

    // 刪除課程 (Service 層已包含報名檢查)
    @Operation(summary = "刪除課程 (需admin)")
    @PreAuthorize("hasAuthority('admin')")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteCourse(@PathVariable Integer id) {
        logger.info("收到刪除課程 ID 為 {} 的請求。", id);
        try {
            courseService.deleteCourse(id);
            logger.info("課程 ID {} 刪除成功。", id);
            return ResponseEntity.noContent().build();
        } catch (EntityNotFoundException e) {
              logger.warn("嘗試刪除課程 ID 為 {}，但未找到。", id, e);
              return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        } catch (IllegalStateException e) {
              logger.warn("由於存在活躍報名/預約，無法刪除課程 ID {}。", id, e);
             return ResponseEntity.status(HttpStatus.CONFLICT).body(null);
        } catch (Exception e) {
              logger.error("刪除課程 ID {} 時發生錯誤。", id, e);
              return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null);
         }
     }

    // 依照課程名稱查詢。
    @Operation(summary = "依照課程名稱查詢")
    @GetMapping("/course_search")
    public ResponseEntity<List< CourseResponse>> searchCoursesByCourseName(@RequestParam String name) {
          logger.info("收到依課程名稱查詢請求：{}", name);
          List< CourseResponse> courses = courseService.searchCoursesByCourseName(name);
          logger.info("返回 {} 個匹配名稱 '{}' 的課程。", courses.size(), name);
          return ResponseEntity.ok(courses);
     }

    // 依照教練ID查詢。
    @Operation(summary = "依照教練ID查詢")
    @GetMapping("/coach")
    public ResponseEntity<List< CourseResponse>> findByCoachId(@RequestParam Integer coachId) {
          logger.info("收到依教練 ID 查詢請求：{}", coachId);
          List< CourseResponse> courses = courseService.findByCoachId(coachId);
          logger.info("返回教練 ID {} 的 {} 個課程。", coachId, courses.size());
          return ResponseEntity.ok(courses);
     }

    // 依照教練名字查詢。
    @Operation(summary = "依照教練名字查詢")
    @GetMapping("/coach_search")
    public ResponseEntity<List< CourseResponse>> searchCoursesByCoachName(@RequestParam String coachName) {
          logger.info("收到依教練名稱查詢請求：{}", coachName);
          List< CourseResponse> courses = courseService.searchCoursesByCoachName(coachName);
          logger.info("返回 {} 個匹配教練名稱 '{}' 的課程。", courses.size(), coachName);
          return ResponseEntity.ok(courses);
     }

	// 查詢特定星期幾的課程。
    @Operation(summary = "依照星期查詢")
    @GetMapping("/day/{dayOfWeek}")
    public ResponseEntity<List< CourseResponse>> getCoursesByDayOfWeek(@PathVariable Integer dayOfWeek) {
          logger.info("收到依星期查詢課程請求：{}", dayOfWeek);
          List< CourseResponse> courses = courseService.getCoursesByDayOfWeek(dayOfWeek);
          logger.info("返回星期 {} 的 {} 個課程。", dayOfWeek, courses.size());
          return ResponseEntity.ok(courses);
     }

    // 依照時段查詢課程的端點
    @Operation(summary = "依照日期時間範圍查詢課程")
    @GetMapping("/date-time-range")
    public ResponseEntity<List<CourseResponse>> getCoursesByDateTimeRange(
            @RequestParam LocalDateTime startTime,
            @RequestParam LocalDateTime endTime) {
         logger.info("收到依日期時間範圍查詢課程請求：{} 到 {}。", startTime, endTime);
         // 調用 Service 方法，現在傳入 LocalDateTime
         List<CourseResponse> courses = courseService.getCoursesByDateTimeRange(startTime, endTime);
         logger.info("返回日期時間範圍 {} 到 {} 的 {} 個課程。", startTime, endTime, courses.size());
         return ResponseEntity.ok(courses);
     }
}
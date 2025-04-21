package com.healthmanagement.controller.course;

import com.healthmanagement.dto.course.CourseRequest;
import com.healthmanagement.dto.course.CourseResponse;
import com.healthmanagement.model.course.Course;
import com.healthmanagement.service.course.CourseService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.dao.EmptyResultDataAccessException;
import jakarta.persistence.EntityNotFoundException;
import java.time.LocalTime;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@RestController
@RequestMapping("/api/courses")
@CrossOrigin(origins = "*") // 允許 Vue 前端的跨域請求
@Tag(name = "課程管理", description = "課程管理API")
public class CourseController {

	private static final Logger logger = LoggerFactory.getLogger(CourseController.class);

    private final CourseService courseService;

    @Autowired
    public CourseController(CourseService courseService) {
        this.courseService = courseService;
    }

	// 查詢所有課程
    @Operation(summary = "查詢所有課程")
    @GetMapping
    public ResponseEntity<List<Course>> getAllCourses() {
        logger.info("Received request to get all courses.");
        List<Course> courses = courseService.getAllCourses();
        logger.info("Returning {} courses.", courses.size());
        return ResponseEntity.ok(courses);
    }

	// 依照課程ID查詢。
    @Operation(summary = "依照課程ID查詢")
    @GetMapping("/{id}")
    public ResponseEntity<CourseResponse> getCourseById(@PathVariable Integer id) {
        logger.info("Received request to get course with ID: {}", id);
        try {
            CourseResponse course = courseService.getCourseById(id);
            logger.info("Returning course with ID: {}", id);
            return ResponseEntity.ok(course);
        } catch (EntityNotFoundException e) {
            logger.warn("Course with ID {} not found.", id, e);
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(null);
        } catch (Exception e) {
            logger.error("Error retrieving course with ID: {}", id, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null);
        }
    }

    // 新增課程
    @Operation(summary = "新增課程 (需admin)")
    @PreAuthorize("hasAuthority('admin')")
    @PostMapping
    public ResponseEntity<CourseResponse> createCourse(@RequestBody CourseRequest courseRequest) {
        logger.info("Received request to create course: {}", courseRequest.getName());
        try {
            CourseResponse createdCourse = courseService.createCourse(courseRequest);
            logger.info("Course created successfully with ID: {}", createdCourse.getId());
            return ResponseEntity.status(HttpStatus.CREATED).body(createdCourse);
        } catch (Exception e) {
             logger.error("Error creating course: {}", courseRequest.getName(), e);
             return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null);
        }
    }

    // 修改課程
    @Operation(summary = "修改課程 (需admin)")
    @PreAuthorize("hasAuthority('admin')")
    @PutMapping("/{id}")
    public ResponseEntity<CourseResponse> updateCourse(@PathVariable Integer id, @RequestBody CourseRequest courseRequest) {
        logger.info("Received request to update course with ID: {}", id);
        try {
            CourseResponse updatedCourse = courseService.updateCourse(id, courseRequest);
            logger.info("Course with ID {} updated successfully.", id);
            return ResponseEntity.ok(updatedCourse);
        } catch (EntityNotFoundException e) {
             logger.warn("Attempted to update course with ID {} but not found.", id, e);
             return ResponseEntity.status(HttpStatus.NOT_FOUND).body(null);
        } catch (Exception e) {
             logger.error("Error updating course with ID: {}", id, e);
             return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null);
        }
    }

    // 刪除課程
    @Operation(summary = "刪除課程 (需admin)")
    @PreAuthorize("hasAuthority('admin')")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteCourse(@PathVariable Integer id) {
        logger.info("Received request to delete course with ID: {}", id);
        try {
            courseService.deleteCourse(id);
            logger.info("Course with ID {} deleted successfully.", id);
            return ResponseEntity.noContent().build();
        } catch (EmptyResultDataAccessException e) {
             logger.warn("Attempted to delete course with ID {} but not found.", id, e);
             return ResponseEntity.status(HttpStatus.NOT_FOUND).body(null);
        } catch (Exception e) {
             logger.error("Error deleting course with ID: {}", id, e);
             return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null);
        }
    }

    // 依照課程名稱查詢。
    @Operation(summary = "依照課程名稱查詢")
    @GetMapping("/course_search")
    public ResponseEntity<List<CourseResponse>> searchCoursesByCourseName(@RequestParam String name) {
         logger.info("Received request to search courses by name: {}", name);
         List<CourseResponse> courses = courseService.searchCoursesByCourseName(name);
         logger.info("Returning {} courses matching name '{}'.", courses.size(), name);
         return ResponseEntity.ok(courses);
    }

    // 依照教練ID查詢。
    @Operation(summary = "依照教練ID查詢")
    @GetMapping("/coach")
    public ResponseEntity<List<CourseResponse>> findByCoachId(@RequestParam Integer coachId) {
         logger.info("Received request to find courses by coach ID: {}", coachId);
         List<CourseResponse> courses = courseService.findByCoachId(coachId);
         logger.info("Returning {} courses for coach ID {}.", courses.size(), coachId);
         return ResponseEntity.ok(courses);
    }

    // 依照教練名字查詢。
    @Operation(summary = "依照教練名字查詢")
    @GetMapping("/coach_search")
    public ResponseEntity<List<CourseResponse>> searchCoursesByCoachName(@RequestParam String coachName) {
         logger.info("Received request to search courses by coach name: {}", coachName);
         List<CourseResponse> courses = courseService.searchCoursesByCoachName(coachName);
         logger.info("Returning {} courses matching coach name '{}'.", courses.size(), coachName);
         return ResponseEntity.ok(courses);
    }

	// 查詢特定星期幾的課程。
    @Operation(summary = "依照星期查詢")
    @GetMapping("/day/{dayOfWeek}")
    public ResponseEntity<List<CourseResponse>> getCoursesByDayOfWeek(@PathVariable Integer dayOfWeek) {
         logger.info("Received request to get courses by day of week: {}", dayOfWeek);
         List<CourseResponse> courses = courseService.getCoursesByDayOfWeek(dayOfWeek);
         logger.info("Returning {} courses for day of week {}.", courses.size(), dayOfWeek);
         return ResponseEntity.ok(courses);
    }

    // 依照時段查詢課程的端點
    @Operation(summary = "依照時段查詢課程")
    @GetMapping("/time-slot")
    public ResponseEntity<List<CourseResponse>> getCoursesByTimeSlot(
            @RequestParam LocalTime startTime,
            @RequestParam LocalTime endTime) {
        logger.info("Received request to get courses by time slot: {} to {}", startTime, endTime);
        List<CourseResponse> courses = courseService.getCoursesByTimeSlot(startTime, endTime);
        logger.info("Returning {} courses for time slot {} to {}.", courses.size(), startTime, endTime);
        return ResponseEntity.ok(courses);
    }
}
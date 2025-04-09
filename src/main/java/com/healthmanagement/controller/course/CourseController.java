// CourseController.java
package com.healthmanagement.controller.course;

import com.healthmanagement.dto.course.CourseRequest;
import com.healthmanagement.dto.course.CourseResponse;
import com.healthmanagement.model.course.Coach;
import com.healthmanagement.model.course.Course;
import com.healthmanagement.service.course.CourseService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.persistence.EntityManager;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/courses")
@CrossOrigin(origins = "*") // 
// 允許 Vue 前端的跨域請求
@Tag(name = "Courses", description = "Course APIs")
public class CourseController {

    @Autowired
    private CourseService courseService;

    @Operation(summary = "Get all courses")
    @GetMapping
    public List<Course> getAllCourses() {
        return courseService.getAllCourses();
    }

    @Operation(summary = "Get course by ID")
    @GetMapping("/{id}")
    public CourseResponse getCourseById(@PathVariable Integer id) {
        return courseService.getCourseById(id);
    }

    @Autowired
    private EntityManager entityManager;

    @Operation(summary = "Create a new course")
    @PostMapping
    public CourseResponse createCourse(@RequestBody CourseRequest courseRequest) {
        // 使用 EntityManager 取得對應 coach 的代理對象（只會設定 ID，不查資料庫）
        Coach coachRef = entityManager.getReference(Coach.class, courseRequest.getCoachId());

        // 建立課程
        Course course = new Course();
        course.setName(courseRequest.getName());
        course.setDescription(courseRequest.getDescription());
        course.setDate(courseRequest.getDate());
        course.setCoach(coachRef); // 設定代理對象
        course.setDuration(courseRequest.getDuration());
        course.setMaxCapacity(courseRequest.getMaxCapacity());

        // 儲存課程
        Course savedCourse = courseService.createCourse(course);

        // 這時候 savedCourse.getCoach() 會是一個完整的實體，可以取得 name 等資訊
        return new CourseResponse(
                savedCourse.getId(),
                savedCourse.getName(),
                savedCourse.getDescription(),
                savedCourse.getCoach().getId(),
                savedCourse.getCoach().getName(),
                savedCourse.getDate(),
                savedCourse.getDuration(),
                savedCourse.getMaxCapacity()
        );
    }

    @Operation(summary = "Update a course by course ID")
    @PutMapping("/{id}")
    public CourseResponse updateCourse(@PathVariable Integer id, @RequestBody CourseRequest courseRequest) {
        // 取得原有的 Course
        Course existingCourse = courseService.getById(id);
        if (existingCourse == null) {
            throw new RuntimeException("Course not found with ID: " + id);
        }
        
        // 更新基本欄位
        existingCourse.setName(courseRequest.getName());
        existingCourse.setDescription(courseRequest.getDescription());
        existingCourse.setDate(courseRequest.getDate());
        existingCourse.setDuration(courseRequest.getDuration());
        existingCourse.setMaxCapacity(courseRequest.getMaxCapacity());

        // 利用 EntityManager 根據 coachId 取得 Coach 代理對象
        Coach coachRef = entityManager.getReference(Coach.class, courseRequest.getCoachId());
        existingCourse.setCoach(coachRef);

        // 呼叫 Service 更新課程
        Course savedCourse = courseService.updateCourse(id, existingCourse);
        
        // 回傳 CourseResponse DTO
        return new CourseResponse(
                savedCourse.getId(),
                savedCourse.getName(),
                savedCourse.getDescription(),
                savedCourse.getCoach().getId(),
                savedCourse.getCoach().getName(),
                savedCourse.getDate(),
                savedCourse.getDuration(),
                savedCourse.getMaxCapacity()
        );
    }

    @Operation(summary = "Delete a course by course ID")
    @DeleteMapping("/{id}")
    public void deleteCourse(@PathVariable Integer id) {
        courseService.deleteCourse(id);
    }

    @Operation(summary = "Search courses by course name")
    @GetMapping("/course_search")
    public List<CourseResponse> searchCoursesWithCoach(@RequestParam String name) {
        return courseService.searchCoursesByCourseName(name);
    }

    @Operation(summary = "Search courses by coach ID")
    @GetMapping("/coach")
    public List<CourseResponse> findByCoach(@RequestParam Integer coachId) {
        return courseService.findByCoachId(coachId);
    }

    
    @Operation(summary = "Search courses by coach name")
    @GetMapping("/coach_search")
    public List<CourseResponse> searchCoursesWithCoachName(@RequestParam String coachName) {
        return courseService.searchCoursesByCoachName(coachName);
    }
}

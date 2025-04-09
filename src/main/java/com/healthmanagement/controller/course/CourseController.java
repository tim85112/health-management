// CourseController.java
package com.healthmanagement.controller.course;

import com.healthmanagement.dto.course.CourseRequest;
import com.healthmanagement.dto.course.CourseResponse;
import com.healthmanagement.model.course.Course;
import com.healthmanagement.service.course.CourseService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/courses")
@CrossOrigin(origins = "*") // // 允許 Vue 前端的跨域請求
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

    @Operation(summary = "Create a new course")
    @PostMapping
    public CourseResponse createCourse(@RequestBody CourseRequest courseRequest) {
        return courseService.createCourse(courseRequest);
    }

    @Operation(summary = "Update a course by course ID")
    @PutMapping("/{id}")
    public CourseResponse updateCourse(@PathVariable Integer id, @RequestBody CourseRequest courseRequest) {
        return courseService.updateCourse(id, courseRequest);
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

// CourseController.java
package com.healthmanagement.controller.course;

import com.healthmanagement.dto.course.CourseRequest;
import com.healthmanagement.dto.course.CourseResponse;
import com.healthmanagement.model.course.Course;
import com.healthmanagement.service.course.CourseService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/courses")
@CrossOrigin(origins = "*") // // 允許 Vue 前端的跨域請求
@Tag(name = "課程管理", description = "課程管理API")
public class CourseController {

    @Autowired
    private CourseService courseService;

    @Operation(summary = "查詢所有課程")
    @GetMapping
    public List<Course> getAllCourses() {
        return courseService.getAllCourses();
    }

    @Operation(summary = "依照課程ID查詢")
    @GetMapping("/{id}")
    public CourseResponse getCourseById(@PathVariable Integer id) {
        return courseService.getCourseById(id);
    }
    	
    @Operation(summary = "新增課程()")
    @PreAuthorize("hasAuthority('admin')") // 只有 admin 可以新增
    @PostMapping
    public CourseResponse createCourse(@RequestBody CourseRequest courseRequest) {
        return courseService.createCourse(courseRequest);
    }

    @Operation(summary = "修改課程")
    @PreAuthorize("hasAuthority('admin')") // 只有 admin 可以修改
    @PutMapping("/{id}")
    public CourseResponse updateCourse(@PathVariable Integer id, @RequestBody CourseRequest courseRequest) {
        return courseService.updateCourse(id, courseRequest);
    }

    @Operation(summary = "刪除課程")
    @PreAuthorize("hasAuthority('admin')") // 只有 admin 可以刪除
    @DeleteMapping("/{id}")
    public void deleteCourse(@PathVariable Integer id) {
        courseService.deleteCourse(id);
    }

    @Operation(summary = "依照課程名稱查詢")
    @GetMapping("/course_search")
    public List<CourseResponse> searchCoursesWithCoach(@RequestParam String name) {
        return courseService.searchCoursesByCourseName(name);
    }

    @Operation(summary = "依照教練ID查詢")
    @GetMapping("/coach")
    public List<CourseResponse> findByCoach(@RequestParam Integer coachId) {
        return courseService.findByCoachId(coachId);
    }


    @Operation(summary = "依照教練名字查詢")
    @GetMapping("/coach_search")
    public List<CourseResponse> searchCoursesWithCoachName(@RequestParam String coachName) {
        return courseService.searchCoursesByCoachName(coachName);
    }

    // 新增查詢特定星期幾的課程
    @Operation(summary = "依照星期查詢")
    @GetMapping("/day/{dayOfWeek}")
    public List<CourseResponse> getCoursesByDayOfWeek(@PathVariable Integer dayOfWeek) {
        return courseService.getCoursesByDayOfWeek(dayOfWeek);
    }
}
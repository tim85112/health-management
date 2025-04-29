package com.healthmanagement.service.course;

import com.healthmanagement.dto.course.CourseRequest;
import com.healthmanagement.dto.course.CourseResponse;
import com.healthmanagement.model.course.Course;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface CourseService {
    List<CourseResponse> getAllCourses();
    CourseResponse getCourseById(Integer id);
    Course getById(Integer id);
    CourseResponse createCourse(CourseRequest courseRequest);
    CourseResponse updateCourse(Integer id, CourseRequest courseRequest);
    void deleteCourse(Integer id);
    List<CourseResponse> searchCoursesByCourseName(String name);
    List<CourseResponse> searchCoursesByCoachName(String coachName);
    List<CourseResponse> getCoursesByDayOfWeek(Integer dayOfWeek);
    List<CourseResponse> getCoursesByDateTimeRange(LocalDateTime startTime, LocalDateTime endTime);
    Page<CourseResponse> findByCoachId(Integer coachId, Pageable pageable);
}
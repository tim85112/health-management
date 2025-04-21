package com.healthmanagement.service.course;

import com.healthmanagement.dto.course.CourseRequest;
import com.healthmanagement.dto.course.CourseResponse;
import com.healthmanagement.model.course.Course;

import java.time.LocalTime;
import java.util.List;

public interface CourseService {
    List<Course> getAllCourses();
    CourseResponse getCourseById(Integer id);
    Course getById(Integer id);
    CourseResponse createCourse(CourseRequest courseRequest);
    CourseResponse updateCourse(Integer id, CourseRequest courseRequest);
    void deleteCourse(Integer id);
    List<CourseResponse> findByCoachId(Integer coachId);
    List<CourseResponse> searchCoursesByCourseName(String name);
    List<CourseResponse> searchCoursesByCoachName(String coachName);
    List<CourseResponse> getCoursesByDayOfWeek(Integer dayOfWeek);
    List<CourseResponse> getCoursesByTimeSlot(LocalTime startTime, LocalTime endTime);
}
package com.healthmanagement.service.course;

import com.healthmanagement.dto.course.CourseResponse;
import com.healthmanagement.model.course.Course;

import java.util.List;

public interface CourseService {
    List<Course> getAllCourses();
    CourseResponse getCourseById(Integer id);
    Course getById(Integer id);
    Course createCourse(Course course);
    Course updateCourse(Integer id, Course course);
    void deleteCourse(Integer id);
    List<CourseResponse> findByCoachId(Integer coachId);
    List<CourseResponse> searchCoursesByCourseName(String name);
    List<CourseResponse> searchCoursesByCoachName(String coachName);

}

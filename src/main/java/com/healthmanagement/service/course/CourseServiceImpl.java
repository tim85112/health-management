package com.healthmanagement.service.course;

import com.healthmanagement.dao.course.CourseDAO;
import com.healthmanagement.dto.course.CourseRequest;
import com.healthmanagement.dto.course.CourseResponse;
import com.healthmanagement.model.course.Course;
import com.healthmanagement.model.member.User;

import jakarta.persistence.EntityManager;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class CourseServiceImpl implements CourseService {

    @Autowired
    private CourseDAO courseDAO;

    @Autowired
    private EntityManager entityManager;

    @Override
    public List<Course> getAllCourses() {
        return courseDAO.findAll();
    }

    @Override
    public CourseResponse getCourseById(Integer id) {
        Optional<Course> optional = courseDAO.findById(id);
        return optional.map(this::convertToCourseResponse).orElse(null);
    }

    @Override
    public Course getById(Integer id) {
        return courseDAO.findById(id).orElse(null);
    }

    @Override
    public CourseResponse createCourse(CourseRequest courseRequest) {
        // 使用 User Model 的 EntityManager 引用
        User coachRef = entityManager.getReference(User.class, courseRequest.getCoachId());
        Course course = new Course();
        course.setName(courseRequest.getName());
        course.setDescription(courseRequest.getDescription());
        course.setDayOfWeek(courseRequest.getDayOfWeek()); // 設定星期幾
        course.setStartTime(courseRequest.getStartTime()); // 設定開始時間
        course.setCoach(coachRef); // 設定關聯的 User 物件
        course.setDuration(courseRequest.getDuration());
        course.setMaxCapacity(courseRequest.getMaxCapacity());
        Course savedCourse = courseDAO.save(course);
        return convertToCourseResponse(savedCourse);
    }

    @Override
    public CourseResponse updateCourse(Integer id, CourseRequest courseRequest) {
        Optional<Course> optional = courseDAO.findById(id);
        if (optional.isEmpty()) {
            throw new RuntimeException("Course not found with ID: " + id);
        }
        Course existingCourse = optional.get();
        existingCourse.setName(courseRequest.getName());
        existingCourse.setDescription(courseRequest.getDescription());
        existingCourse.setDayOfWeek(courseRequest.getDayOfWeek()); // 設定星期幾
        existingCourse.setStartTime(courseRequest.getStartTime()); // 設定開始時間
        // 使用 User Model 的 EntityManager 引用
        User coachRef = entityManager.getReference(User.class, courseRequest.getCoachId());
        existingCourse.setCoach(coachRef); // 設定關聯的 User 物件
        existingCourse.setDuration(courseRequest.getDuration());
        existingCourse.setMaxCapacity(courseRequest.getMaxCapacity());
        Course updatedCourse = courseDAO.save(existingCourse);
        return convertToCourseResponse(updatedCourse);
    }

    @Override
    public void deleteCourse(Integer id) {
        courseDAO.deleteById(id);
    }

    @Override
    public List<CourseResponse> findByCoachId(Integer coachId) {
        return courseDAO.findByCoachId(coachId).stream()
                .map(this::convertToCourseResponse)
                .collect(Collectors.toList());
    }

    @Override
    public List<CourseResponse> searchCoursesByCourseName(String name) {
        return courseDAO.findByNameContainingIgnoreCase(name).stream()
                .map(this::convertToCourseResponse)
                .collect(Collectors.toList());
    }

    @Override
    public List<CourseResponse> searchCoursesByCoachName(String coachName) {
        return courseDAO.findByCoachNameContainingIgnoreCase(coachName).stream()
                .map(this::convertToCourseResponse)
                .collect(Collectors.toList());
    }

    @Override
    public List<CourseResponse> getCoursesByDayOfWeek(Integer dayOfWeek) {
        return courseDAO.findByDayOfWeek(dayOfWeek).stream()
                .map(this::convertToCourseResponse)
                .collect(Collectors.toList());
    }

    private CourseResponse convertToCourseResponse(Course course) {
        return new CourseResponse(
                course.getId(),
                course.getName(),
                course.getDescription(),
                course.getCoach().getId(), // 從關聯的 User 物件獲取 ID
                course.getCoach().getName(), // 從關聯的 User 物件獲取 Name
                course.getDayOfWeek(), // 新增星期幾
                course.getStartTime(), // 新增開始時間
                course.getDuration(),
                course.getMaxCapacity()
        );
    }
}
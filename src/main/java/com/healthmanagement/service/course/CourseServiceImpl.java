package com.healthmanagement.service.course;

import com.healthmanagement.dao.course.CourseDAO;
import com.healthmanagement.dto.course.CourseRequest;
import com.healthmanagement.dto.course.CourseResponse;
import com.healthmanagement.model.course.Coach;
import com.healthmanagement.model.course.Course;
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
        Coach coachRef = entityManager.getReference(Coach.class, courseRequest.getCoachId());
        Course course = new Course();
        course.setName(courseRequest.getName());
        course.setDescription(courseRequest.getDescription());
        course.setDate(courseRequest.getDate());
        course.setCoach(coachRef);
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
        existingCourse.setDate(courseRequest.getDate());
        Coach coachRef = entityManager.getReference(Coach.class, courseRequest.getCoachId());
        existingCourse.setCoach(coachRef);
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

    private CourseResponse convertToCourseResponse(Course course) {
        return new CourseResponse(
                course.getId(),
                course.getName(),
                course.getDescription(),
                course.getCoach().getId(),
                course.getCoach().getName(),
                course.getDate(),
                course.getDuration(),
                course.getMaxCapacity()
        );
    }
}
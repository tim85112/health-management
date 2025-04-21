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
import java.time.LocalTime;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Service
public class CourseServiceImpl implements CourseService {

    private static final Logger logger = LoggerFactory.getLogger(CourseServiceImpl.class);

    @Autowired
    private CourseDAO courseDAO;

    @Autowired
    private EntityManager entityManager;

    @Override
    public List<Course> getAllCourses() {
         logger.info("Fetching all courses.");
         return courseDAO.findAll();
    }

    @Override
    public CourseResponse getCourseById(Integer id) {
         logger.info("Fetching course with ID: {}", id);
         Optional<Course> optional = courseDAO.findById(id);
         if (optional.isEmpty()) {
             logger.warn("Course not found with ID: {}", id);
             return null;
         }
         return convertToCourseResponse(optional.get());
    }

    @Override
    public Course getById(Integer id) {
        logger.info("Fetching course entity with ID: {}", id);
        Optional<Course> optional = courseDAO.findById(id);
        if (optional.isEmpty()) {
            logger.warn("Course entity not found with ID: {}", id);
            return null;
        }
        return optional.get();
    }


    @Override
    public CourseResponse createCourse(CourseRequest courseRequest) {
         logger.info("Creating new course with name: {}", courseRequest.getName());
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
         logger.info("Course created successfully with ID: {}", savedCourse.getId());
         return convertToCourseResponse(savedCourse);
    }

    @Override
    public CourseResponse updateCourse(Integer id, CourseRequest courseRequest) {
         logger.info("Updating course with ID: {}", id);
         Optional<Course> optional = courseDAO.findById(id);
         if (optional.isEmpty()) {
             logger.warn("Attempted to update course with ID {} but not found.", id);
             throw new RuntimeException("Course not found with ID: " + id);
         }
         Course existingCourse = optional.get();
         existingCourse.setName(courseRequest.getName());
         existingCourse.setDescription(courseRequest.getDescription());
         existingCourse.setDayOfWeek(courseRequest.getDayOfWeek()); // 設定星期幾
         existingCourse.setStartTime(courseRequest.getStartTime()); // 設定開始時間
         User coachRef = entityManager.getReference(User.class, courseRequest.getCoachId());
         existingCourse.setCoach(coachRef); // 設定關聯的 User 物件
         existingCourse.setDuration(courseRequest.getDuration());
         existingCourse.setMaxCapacity(courseRequest.getMaxCapacity());
         Course updatedCourse = courseDAO.save(existingCourse);
         logger.info("Course with ID {} updated successfully.", updatedCourse.getId());
         return convertToCourseResponse(updatedCourse);
    }

    @Override
    public void deleteCourse(Integer id) {
         logger.info("Deleting course with ID: {}", id);
         courseDAO.deleteById(id);
         logger.info("Course with ID {} deleted.", id);
    }

    @Override
    public List<CourseResponse> findByCoachId(Integer coachId) {
         logger.info("Finding courses by coach ID: {}", coachId);
         List<Course> courses = courseDAO.findByCoachId(coachId);
         logger.info("Found {} courses for coach ID {}.", courses.size(), coachId);
         return courses.stream()
             .map(this::convertToCourseResponse)
             .collect(Collectors.toList());
    }

    @Override
    public List<CourseResponse> searchCoursesByCourseName(String name) {
         logger.info("Searching courses by name: {}", name);
         List<Course> courses = courseDAO.findByNameContainingIgnoreCase(name);
         logger.info("Found {} courses matching name '{}'.", courses.size(), name);
         return courses.stream()
             .map(this::convertToCourseResponse)
             .collect(Collectors.toList());
    }

    @Override
    public List<CourseResponse> searchCoursesByCoachName(String coachName) {
         logger.info("Searching courses by coach name: {}", coachName);
         List<Course> courses = courseDAO.findByCoachNameContainingIgnoreCase(coachName);
         logger.info("Found {} courses matching coach name '{}'.", courses.size(), coachName);
         return courses.stream()
             .map(this::convertToCourseResponse)
             .collect(Collectors.toList());
    }

    @Override
    public List<CourseResponse> getCoursesByDayOfWeek(Integer dayOfWeek) {
         logger.info("Fetching courses by day of week: {}", dayOfWeek);
         List<Course> courses = courseDAO.findByDayOfWeek(dayOfWeek);
         logger.info("Found {} courses for day of week {}.", courses.size(), dayOfWeek);
         return courses.stream()
             .map(this::convertToCourseResponse)
             .collect(Collectors.toList());
    }

    @Override
    public List<CourseResponse> getCoursesByTimeSlot(LocalTime startTime, LocalTime endTime) {
         logger.info("Finding courses by time slot: {} to {}", startTime, endTime);
         List<Course> courses = courseDAO.findByStartTimeBetween(startTime, endTime);
         logger.info("Found {} courses for time slot {} to {}.", courses.size(), startTime, endTime);
         return courses.stream()
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
                course.getDayOfWeek(),
                course.getStartTime(),
                course.getDuration(),
                course.getMaxCapacity()
        );
    }
}
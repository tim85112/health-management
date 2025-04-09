package com.healthmanagement.service.course;

import com.healthmanagement.dao.course.CourseDAO;
import com.healthmanagement.dto.course.CourseResponse;
import com.healthmanagement.model.course.Course;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class CourseServiceImpl implements CourseService {

    @Autowired
    private CourseDAO courseDAO;

    @Override
    public List<Course> getAllCourses() {
        return courseDAO.findAll();
    }

    @Override
    public CourseResponse getCourseById(Integer id) {
        Optional<Course> optional = courseDAO.findById(id);
        if (optional.isPresent()) {
            Course course = optional.get();
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
        return null;
    }
    
    @Override
    public Course getById(Integer id) {
        return courseDAO.findById(id).orElse(null);
    }

    @Override
    public Course createCourse(Course course) {
        return courseDAO.save(course);
    }

    @Override
    public Course updateCourse(Integer id, Course updatedCourse) {
        Optional<Course> optional = courseDAO.findById(id);
        if (optional.isPresent()) {
            Course course = optional.get();
            course.setName(updatedCourse.getName());
            course.setDescription(updatedCourse.getDescription());
            course.setDate(updatedCourse.getDate());
            course.setCoach(updatedCourse.getCoach());
            course.setDuration(updatedCourse.getDuration());
            course.setMaxCapacity(updatedCourse.getMaxCapacity());
            return courseDAO.save(course);
        }
        return null;
    }

    @Override
    public void deleteCourse(Integer id) {
    	courseDAO.deleteById(id);
    }

    @Override
    public List<CourseResponse> searchCoursesByCourseName(String name) {
        List<Course> courses = courseDAO.findByNameContainingIgnoreCase(name);
        return courses.stream()
                .map(course -> new CourseResponse(
                        course.getId(),
                        course.getName(),
                        course.getDescription(),
                        course.getCoach().getId(),
                        course.getCoach().getName(),
                        course.getDate(),
                        course.getDuration(),
                        course.getMaxCapacity()
                ))
                .collect(Collectors.toList());
    }

    @Override
    public List<CourseResponse> findByCoachId(Integer coachId) {
        List<Course> courses = courseDAO.findByCoachId(coachId);
        return courses.stream()
                .map(course -> new CourseResponse(
                        course.getId(),
                        course.getName(),
                        course.getDescription(),
                        course.getCoach().getId(),
                        course.getCoach().getName(),
                        course.getDate(),
                        course.getDuration(),
                        course.getMaxCapacity()
                ))
                .collect(Collectors.toList());
    }
    
    @Override
    public List<CourseResponse> searchCoursesByCoachName(String coachName) {
        List<Course> courses = courseDAO.findByCoachNameContainingIgnoreCase(coachName);
        return courses.stream()
                .map(course -> new CourseResponse(
                        course.getId(),
                        course.getName(),
                        course.getDescription(),
                        course.getCoach().getId(),
                        course.getCoach().getName(),
                        course.getDate(),
                        course.getDuration(),
                        course.getMaxCapacity()
                ))
                .collect(Collectors.toList());
    }
}

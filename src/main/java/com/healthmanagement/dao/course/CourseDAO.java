package com.healthmanagement.dao.course;

import com.healthmanagement.model.course.Course;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.EntityGraph; // 確保引入 EntityGraph
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface CourseDAO extends JpaRepository<Course, Integer> {

    @EntityGraph(attributePaths = {"images", "coach"})
    Page<Course> findByCoachId(Integer coachId, Pageable pageable);

    @EntityGraph(attributePaths = {"images", "coach"})
    List<Course> findByNameContainingIgnoreCase(String name);

    @Query("SELECT c FROM Course c JOIN FETCH c.coach coach LEFT JOIN FETCH c.images WHERE LOWER(coach.name) LIKE LOWER(CONCAT('%', :coachName, '%'))")
    List<Course> findByCoachNameContainingIgnoreCase(@Param("coachName") String coachName);

    @EntityGraph(attributePaths = {"images", "coach"})
    List<Course> findByDayOfWeek(Integer dayOfWeek);

    // === 修正: 在 findCoursesWithFilters 上添加 @EntityGraph ===
    @EntityGraph(attributePaths = {"coach", "images"}) // 添加 EntityGraph 以載入 coach 和 images
    @Query("SELECT c FROM Course c WHERE (:offersTrialOption IS NULL OR c.offersTrialOption = :offersTrialOption) AND (:dayOfWeek IS NULL OR c.dayOfWeek = :dayOfWeek)")
    Page<Course> findCoursesWithFilters(Pageable pageable, @Param("offersTrialOption") Boolean offersTrialOption, @Param("dayOfWeek") Integer dayOfWeek);
    // =======================================================

    @Query("SELECT c FROM Course c LEFT JOIN FETCH c.images LEFT JOIN FETCH c.coach")
    List<Course> findAllWithImagesAndCoach();

    @Override
    @EntityGraph(attributePaths = {"images", "coach"})
    Optional<Course> findById(Integer id);
}
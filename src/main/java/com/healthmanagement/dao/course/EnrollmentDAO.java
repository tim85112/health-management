package com.healthmanagement.dao.course;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.healthmanagement.model.course.Course;
import com.healthmanagement.model.course.Enrollment;
import com.healthmanagement.model.member.User;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
public interface EnrollmentDAO extends JpaRepository<Enrollment, Integer> {

    // 查找特定課程的所有報名記錄
    List<Enrollment> findByCourse(Course course);
    
    // 查找特定用戶的所有報名記錄。
    List<Enrollment> findByUser(User user);
    
	// 查找特定課程中具有指定狀態的所有報名記錄。
    List<Enrollment> findByCourseAndStatus(Course course, String status);
    
    // 檢查特定使用者和課程之間是否存在報名記錄，不考慮狀態。
    boolean existsByUserAndCourse(User user, Course course);
    
	// 檢查特定使用者和課程之間是否存在狀態不在指定列表中的報名記錄（例如，檢查是否存在非已取消的報名）。
    boolean existsByUserAndCourseAndStatusNotIn(User user, Course course, List<String> statuses);
    
    // 查找特定使用者和課程中具有指定狀態的報名記錄。
    Optional<Enrollment> findByUserAndCourseAndStatus(User user, Course course, String status);
    
	// 計算特定課程中具有指定狀態的報名記錄數量。
    int countByCourseAndStatus(Course course, String status);
    
    // 查找特定課程中具有指定狀態的所有報名記錄，並按報名時間升序排序（常用於獲取候補名單）。
    List<Enrollment> findByCourseAndStatusOrderByEnrollmentTimeAsc(Course course, String status);
    
    // 精確檢查特定使用者在某課程中是否具有特定狀態（例如 已報名 或 候補中）。
    boolean existsByUserAndCourseAndStatus(User user, Course course, String status);
    
    // 查找具有指定狀態的所有報名記錄。
    List<Enrollment> findByStatus(String status);
    
	// 新增方法以查找多個課程中特定狀態的報名記錄（用於批量獲取需要統計的報名記錄）
    List<Enrollment> findByCourseInAndStatus(Collection<Course> courses, String status);
    
	// 檢查特定課程中是否存在狀態不在指定列表中的報名記錄
    boolean existsByCourseIdAndStatusNotIn(Integer courseId, Collection<String> statuses);
    
	// 查找特定用戶狀態不在指定列表中的所有報名記錄
    List<Enrollment> findByUserAndStatusNotIn(User user, Collection<String> statuses);
    
    // 查找特定課程 ID 列表中，狀態為指定狀態的報名記錄
    // 這個方法是為了解決您遇到的編譯錯誤而添加的
    List<Enrollment> findByCourseIdInAndStatus(List<Integer> courseIds, String status);
    
	// 查找特定使用者對特定課程 ID 列表中，狀態不在指定列表中的活躍報名記錄
    // 這個方法是為了解決您遇到的編譯錯誤而添加的
    List<Enrollment> findByUserAndCourseIdInAndStatusNotIn(User user, List<Integer> courseIds, List<String> statuses);
    
    // 查找特定狀態的報名記錄並支援分頁
    Page<Enrollment> findByStatus(String status, Pageable pageable);
    
    List<Enrollment> findByUser_NameContainingIgnoreCase(String userName);
}
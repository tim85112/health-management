package com.healthmanagement.service.course;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.springframework.data.domain.Page;

import com.healthmanagement.dto.course.EnrollmentDTO;
import com.healthmanagement.dto.course.EnrollmentStatusUpdateDTO;
import com.healthmanagement.model.course.Course;
import com.healthmanagement.dto.course.CourseInfoDTO;

public interface EnrollmentService {
    // 常規課程報名 (已加入 24 小時限制和檢查是否為體驗課程)
    EnrollmentDTO enrollUserToCourse(Integer userId, Integer courseId);
    // 取消常規課程報名 (已加入 24 小時限制和候補自動遞補)
    void cancelEnrollment(Integer enrollmentId);
    // 更新報名狀態 (可能用於後台或特定流程)
    EnrollmentDTO updateEnrollmentStatus(Integer enrollmentId, EnrollmentStatusUpdateDTO updateDTO);
    // 加入候補名單 (此方法可能已不需公共訪問，因為報名邏輯內部處理，但介面保留)
    EnrollmentDTO addWaitlistItem(Integer userId, Integer courseId);
    // 處理候補名單 (可能用於手動觸發或排程，自動遞補已在 cancelEnrollment 中)
    void processWaitlist(Integer courseId);
    // 查詢特定使用者的報名記錄
    List<EnrollmentDTO> getEnrollmentsByUserId(Integer userId);
    // 查詢特定課程的所有報名記錄
    List<EnrollmentDTO> getEnrollmentsByCourseId(Integer courseId);
    // 查詢特定課程是否已滿
    boolean isCourseFull(Integer courseId);
    // 查詢特定使用者是否已有效報名或候補特定課程
    boolean isUserEnrolled(Integer userId, Integer courseId);
    // 查詢特定課程的已報名人數
    int getEnrolledCount(Integer courseId);

    // 修改方法簽名以支援分頁和篩選
    // 查詢課程列表，包含使用者的報名/預約狀態和人數，並支援分頁及體驗課、星期幾篩選
    Page<CourseInfoDTO> getAllCoursesWithUserStatus(Integer userId, Integer page, Integer size, Boolean offersTrialOption, Integer dayOfWeek, String fullnessStatus);

	// 檢查特定課程是否存在活躍的常規報名記錄 (新增用於 CourseService 刪除檢查)
    boolean hasActiveEnrollmentsForCourse(Integer courseId);
    // 獲取給定課程列表中，每個課程的「下一個排程」的體驗預約人數。
    Map<Integer, Integer> getNextOccurrenceBookedTrialCounts(List<Course> courses);

    // 查詢報名紀錄並支援分頁。
    // 這個方法是查詢報名記錄 (Enrollment)，不是課程 (Course)
    Page<EnrollmentDTO> findEnrollmentsPaginated(int page, int pageSize, String status);

    Optional<EnrollmentDTO> findEnrollmentById(Integer id);

    List<EnrollmentDTO> searchEnrollmentsByUserName(String userName);

	// 新增方法：查詢特定使用者是否已預約特定體驗課程
    boolean isUserTrialBooked(Integer userId, Integer courseId);
}
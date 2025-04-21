package com.healthmanagement.dao.course;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.healthmanagement.model.course.TrialBooking;
import com.healthmanagement.model.course.Course; // 需要 Course Entity 來JOIN
import com.healthmanagement.model.member.User;

import java.time.LocalDate;
import java.time.LocalTime; // 雖然 TrialBooking 沒有 start_time，但查詢時可能需要 LocalTime 參數來比對 Course 的 start_time
import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
public interface TrialBookingDAO extends JpaRepository<TrialBooking, Integer> {
    // 查找特定用戶的所有體驗預約
	List<TrialBooking> findByUser(User user);
	
    // 查找特定課程的所有體驗預約 (不分日期、狀態)
	List<TrialBooking> findByCourse(Course course);
	
    // 查找特定用戶在**特定日期**、特定課程的體驗預約 (用於檢查使用者是否已預約某天某課程)
	Optional<TrialBooking> findByUserAndCourseAndBookingDateAndBookingStatusNotIn(User user, Course course, LocalDate bookingDate, List<String> excludedStatuses);
    
	// 查找特定課程在特定日期的所有體驗預約 (用於容量檢查 - 只考慮日期，不考慮具體時間點，因為體驗課該天只有一個固定開始時間)
	List<TrialBooking> findByCourseAndBookingDateAndBookingStatusNotIn(Course course, LocalDate bookingDate, List<String> excludedStatuses);
    
	// 計算特定課程、特定日期、特定時間點(此時間點應指 Course 的 Start_time) 的活躍預約人數
	@Query(value = "SELECT COUNT(t.id) FROM trial_booking t JOIN course c ON t.course_id = c.id WHERE t.course_id = :courseId AND t.booking_date = :bookingDate AND CAST(c.start_time AS TIME) = CAST(:startTime AS TIME) AND t.booking_status NOT IN (:excludedStatuses)", nativeQuery = true)
	int countTrialBookingsByCourseDateActualStartTimeAndStatusNotInNative(
	    @Param("courseId") Integer courseId,
	    @Param("bookingDate") LocalDate bookingDate,
	    @Param("startTime") LocalTime startTime,
	    @Param("excludedStatuses") Collection<String> excludedStatuses
	);
    
	// 查找所有過期的「已預約」體驗預約
	@Query("SELECT t FROM TrialBooking t JOIN t.course c WHERE t.bookingStatus = :status AND (t.bookingDate < :currentDate OR (t.bookingDate = :currentDate AND c.startTime < :currentTime))")
    List<TrialBooking> findPastDueBookedTrialBookings(
        @Param("status") String status,
        @Param("currentDate") LocalDate currentDate,
        @Param("currentTime") LocalTime currentTime
    );
	
    // 根據狀態查找所有體驗預約
	List<TrialBooking> findByBookingStatus(String status);
	
	// 用於根據用戶、姓名、手機號碼、課程、日期和排除狀態查找預約
    Optional<TrialBooking> findByUserAndBookingNameAndBookingPhoneAndCourseAndBookingDateAndBookingStatusNotIn(
        User user,
        String bookingName,
        String bookingPhone,
        Course course,
        LocalDate bookingDate,
        List<String> excludedStatuses
    );
}
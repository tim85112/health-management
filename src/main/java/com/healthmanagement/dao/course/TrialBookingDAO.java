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
 // 查找特定課程在指定日期之後，狀態不在排除列表中的，第一個體驗預約實例
    // 這個方法可以找到特定日期之後的最近一次體驗預約記錄，用於獲取其計數
    @Query("SELECT tb FROM TrialBooking tb WHERE tb.course = :course AND tb.bookingStatus NOT IN :excludedStatuses AND tb.bookingDate >= :startDate ORDER BY tb.bookingDate ASC, tb.course.startTime ASC")
    List<TrialBooking> findFirstActiveBookingForCourseAfterDate(
        @Param("course") Course course,
        @Param("excludedStatuses") Collection<String> excludedStatuses,
        @Param("startDate") LocalDate startDate,
        org.springframework.data.domain.Pageable pageable // 使用 Pageable 限制只返回第一個
    );

    // 計算特定課程在指定日期之後，狀態不在排除列表中的，第一個體驗預約實例的人數
    // 結合上面的查詢，這個方法用於獲取下一個排程的體驗預約人數
    // 注意：這個計數需要根據課程的 booking_date 和 course 的 start_time 精確匹配
    // 可以重用現有的 countTrialBookingsByCourseDateActualStartTimeAndStatusNotInNative 方法，但需要在 Service 層先找到下一個排程日期

    
	 // 查找特定使用者對特定課程的**所有未來活躍**的體驗預約記錄
	 // 用於在 CourseInfoDTO 中判斷使用者是否有預約以及獲取 bookingId
	 @Query("SELECT tb FROM TrialBooking tb WHERE tb.user = :user AND tb.course = :course AND tb.bookingStatus NOT IN :excludedStatuses AND tb.bookingDate >= :currentDate")
	 Optional<TrialBooking> findFirstActiveBookingByUserAndCourseAfterDate(
	     @Param("user") User user,
	     @Param("course") Course course,
	     @Param("excludedStatuses") List<String> excludedStatuses, // *** 將 Collection 改為 List ***
	     @Param("currentDate") LocalDate currentDate
	 );

    // 檢查特定課程是否存在狀態不在指定列表中的體驗預約 (用於 delete check in CourseServiceImpl)
    boolean existsByCourseIdAndBookingStatusNotIn(Integer courseId, Collection<String> excludedStatuses);

    // 可能需要一個方法來獲取多個課程在多個不同日期+時間組合下的體驗預約總數
    // 這個會比較複雜，可能會留在 Service 層處理分組和計數，或者需要一個 Native Query
    // 例如：Map<Integer, Long> countActiveTrialBookingsForSpecificOccurrences(List<CourseOccurrenceKey> occurrenceKeys, Collection<String> excludedStatuses);
    // CourseOccurrenceKey 可以是包含 courseId, bookingDate, startTime 的自定義類別
    
	// 查找特定使用者對一批課程的未來活躍體驗預約記錄
    List<TrialBooking> findByUserAndCourseIdInAndBookingStatusNotInAndBookingDateGreaterThanEqual(
        User user,
        Collection<Integer> courseIds, // 使用 Collection<Integer> 更通用且可能更高效
        Collection<String> excludedStatuses,
        LocalDate currentDate
    );
}
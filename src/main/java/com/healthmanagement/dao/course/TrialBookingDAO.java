package com.healthmanagement.dao.course;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.healthmanagement.model.course.TrialBooking;
import com.healthmanagement.model.course.Course;
import com.healthmanagement.model.member.User;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
public interface TrialBookingDAO extends JpaRepository<TrialBooking, Integer> {

    // ... 其他已有的方法 ...

    // 查找特定使用者和課程在某日期之後，狀態不在指定列表中的第一筆活躍預約 (名稱不變)
    @Query("SELECT tb FROM TrialBooking tb WHERE tb.user = :user AND tb.course = :course AND tb.bookingStatus NOT IN :excludedStatuses AND tb.bookingDate >= :currentDate")
    Optional<TrialBooking> findFirstByUserAndCourseAndBookingDateAfterAndStatusNotIn(
        @Param("user") User user,
        @Param("course") Course course,
        @Param("excludedStatuses") Collection<String> excludedStatuses, // 參數名稱與 @Query 保持一致更安全
        @Param("currentDate") LocalDate currentDate // 參數名稱與 @Query 保持一致更安全
    );

    // *** MODIFICATION: 方法名稱改為反映 bookingEmail ***
    // 用於匿名使用者重複預約檢查 (基於 bookingEmail 和其他詳細資訊)
    Optional<TrialBooking> findActiveBookingByBookingEmailAndBookingNameAndBookingPhoneAndCourseAndBookingDateAndBookingStatusNotIn(
        String bookingEmail, // MODIFICATION: 參數名稱改為 bookingEmail
        String bookingName,
        String bookingPhone,
        Course course,
        LocalDate bookingDate,
        Collection<String> excludedStatuses
    );


    // ... 其他您提供的 DAO 中已經存在且名稱無需因 email 修改的方法 ...

    // 例如：
    List<TrialBooking> findByUser(User user);
    List<TrialBooking> findByCourse(Course course);
    boolean existsByCourseIdAndBookingStatusNotIn(Integer courseId, Collection<String> excludedStatuses);
    List<TrialBooking> findByBookingStatus(String status);

    // 查找特定課程在指定日期之後... (這個方法可能需要在 @Query 中使用 tb.booking_email)
    @Query("SELECT tb FROM TrialBooking tb WHERE tb.course = :course AND tb.bookingStatus NOT IN :excludedStatuses AND tb.bookingDate >= :startDate ORDER BY tb.bookingDate ASC, tb.course.startTime ASC")
    List<TrialBooking> findFirstActiveBookingForCourseAfterDate(
        @Param("course") Course course,
        @Param("excludedStatuses") Collection<String> excludedStatuses,
        @Param("startDate") LocalDate startDate,
        org.springframework.data.domain.Pageable pageable
    );


    // 查找所有過期的「已預約」體驗預約 (@Query 中應使用 tb.booking_email)
    @Query("SELECT t FROM TrialBooking t JOIN t.course c WHERE t.bookingStatus = :status AND (t.bookingDate < :currentDate OR (t.bookingDate = :currentDate AND c.startTime < :currentTime))")
    List<TrialBooking> findPastDueBookedTrialBookings(
        @Param("status") String status,
        @Param("currentDate") LocalDate currentDate,
        @Param("currentTime") LocalTime currentTime
    );

    // 計算特定課程、日期、時間點的活躍預約人數 (Native Query，應使用 t.booking_email)
    // 注意：Native Query 需要手動修改欄位名稱
    @Query(value = "SELECT COUNT(t.id) FROM trial_booking t JOIN course c ON t.course_id = c.id WHERE t.course_id = :courseId AND t.booking_date = :bookingDate AND CAST(c.start_time AS TIME) = CAST(:startTime AS TIME) AND t.booking_status NOT IN (:excludedStatuses)", nativeQuery = true)
    int countTrialBookingsByCourseDateActualStartTimeAndStatusNotInNative(
        @Param("courseId") Integer courseId,
        @Param("bookingDate") LocalDate bookingDate,
        @Param("startTime") LocalTime startTime,
        @Param("excludedStatuses") Collection<String> excludedStatuses
    );
    // 注意：上面的 Native Query 中，如果 where 條件或 select 中使用了 email 欄位，需要手動改為 booking_email。
    // 在這個特定的 count Query 中，沒有直接使用 email，所以 Query 本身可能不需要改欄位名，但如果其他 Native Query 有用到就要改。


    // 查找特定使用者對一批課程的未來活躍體驗預約記錄 (這個方法的實現可能需要修改，如果底層用到 email)
    List<TrialBooking> findByUserAndCourseIdInAndBookingStatusNotInAndBookingDateGreaterThanEqual(
        User user,
        Collection<Integer> courseIds,
        Collection<String> excludedStatuses,
        LocalDate currentDate
    );


    // 獲取所有體驗預約紀錄 (支援分頁和篩選) - @Query 中應使用 tb.booking_email
    @Query("SELECT tb FROM TrialBooking tb LEFT JOIN tb.user u LEFT JOIN tb.course c " +
           "WHERE (:bookingStatus IS NULL OR tb.bookingStatus = :bookingStatus) " +
           "AND (:courseId IS NULL OR c.id = :courseId) " +
           "AND (" +
           "   (:userId IS NULL AND tb.user IS NULL)" +
           "   OR (:userId IS NULL AND tb.user IS NOT NULL)" +
           "   OR (:userId IS NOT NULL AND u.id = :userId)" +
           ")")
    Page<TrialBooking> findWithFilters(
        Pageable pageable,
        @Param("bookingStatus") String bookingStatus,
        @Param("courseId") Integer courseId,
        @Param("userId") Integer userId
    );

    // 如果您有其他使用到 email 欄位的 @Query，請確保將其中的 email 改為 booking_email
    // 例如：
    // @Query("SELECT tb FROM TrialBooking tb WHERE tb.bookingEmail = :email") // <--- 錯誤，應該是 bookingEmail
    // Optional<TrialBooking> findByEmail(@Param("email") String email); // <--- 方法名和參數名也應該改
    // 修改為：
    // @Query("SELECT tb FROM TrialBooking tb WHERE tb.bookingEmail = :bookingEmail") // 使用新的欄位名
    // Optional<TrialBooking> findByBookingEmail(@Param("bookingEmail") String bookingEmail); // 新的方法名和參數名
    
    // *** 新增: 根據 bookingName 查詢體驗預約記錄列表的方法簽名 ***
    // Spring Data JPA 會自動根據方法名稱解析出查詢邏輯
    // findByBookingName 表示根據 TrialBooking 實體的 bookingName 屬性查詢
    // Containing 表示模糊匹配 (LIKE %...)
    // IgnoreCase 表示忽略大小寫
    List<TrialBooking> findByBookingNameContainingIgnoreCase(String bookingName);

}
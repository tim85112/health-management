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

      // 查找特定使用者和課程在某日期之後，狀態不在指定列表中的第一筆活躍預約
      @Query("SELECT tb FROM TrialBooking tb WHERE tb.user = :user AND tb.course = :course AND tb.bookingStatus NOT IN :excludedStatuses AND tb.bookingDate >= :currentDate")
      Optional<TrialBooking> findFirstByUserAndCourseAndBookingDateAfterAndStatusNotIn(
            @Param("user") User user,
            @Param("course") Course course,
            @Param("excludedStatuses") Collection<String> excludedStatuses,
            @Param("currentDate") LocalDate currentDate
      );

      // 用於匿名使用者重複預約檢查 (基於 bookingEmail 和其他詳細資訊)
      Optional<TrialBooking> findActiveBookingByBookingEmailAndBookingNameAndBookingPhoneAndCourseAndBookingDateAndBookingStatusNotIn(
            String bookingEmail,
            String bookingName,
            String bookingPhone,
            Course course,
            LocalDate bookingDate,
            Collection<String> excludedStatuses
      );

      List<TrialBooking> findByUser(User user);
      List<TrialBooking> findByCourse(Course course);
      boolean existsByCourseIdAndBookingStatusNotIn(Integer courseId, Collection<String> excludedStatuses);

      // 查找所有過期的「已預約」體驗預約
      @Query("SELECT t FROM TrialBooking t JOIN t.course c WHERE t.bookingStatus = :status AND (t.bookingDate < :currentDate OR (t.bookingDate = :currentDate AND c.startTime < :currentTime))")
      List<TrialBooking> findPastDueBookedTrialBookings(
            @Param("status") String status,
            @Param("currentDate") LocalDate currentDate,
            @Param("currentTime") LocalTime currentTime
      );

      // 計算特定課程、日期、時間點的活躍預約人數 (Native Query)
      @Query(value = "SELECT COUNT(t.id) FROM trial_booking t JOIN course c ON t.course_id = c.id WHERE t.course_id = :courseId AND t.booking_date = :bookingDate AND CAST(c.start_time AS TIME) = CAST(:startTime AS TIME) AND t.booking_status NOT IN (:excludedStatuses)", nativeQuery = true)
      int countTrialBookingsByCourseDateActualStartTimeAndStatusNotInNative(
            @Param("courseId") Integer courseId,
            @Param("bookingDate") LocalDate bookingDate,
            @Param("startTime") LocalTime startTime,
            @Param("excludedStatuses") Collection<String> excludedStatuses
      );

      // 查找特定使用者對一批課程的未來活躍體驗預約記錄
      List<TrialBooking> findByUserAndCourseIdInAndBookingStatusNotInAndBookingDateGreaterThanEqual(
            User user,
            Collection<Integer> courseIds,
            Collection<String> excludedStatuses,
            LocalDate currentDate
      );

      // 獲取所有體驗預約紀錄 (支援分頁和篩選)
      @Query("SELECT tb FROM TrialBooking tb LEFT JOIN tb.user u LEFT JOIN tb.course c " +
                 "WHERE (:bookingStatus IS NULL OR tb.bookingStatus = :bookingStatus) " +
                 "AND (:courseId IS NULL OR c.id = :courseId) " +
                 "AND (" +
                 "     (:userId IS NULL AND tb.user IS NULL)" +
                 "     OR (:userId IS NULL AND tb.user IS NOT NULL)" +
                 "     OR (:userId IS NOT NULL AND u.id = :userId)" +
                 ")")
      Page<TrialBooking> findWithFilters(
            Pageable pageable,
            @Param("bookingStatus") String bookingStatus,
            @Param("courseId") Integer courseId,
            @Param("userId") Integer userId
      );

      // 根據 bookingName 查詢體驗預約記錄列表
      List<TrialBooking> findByBookingNameContainingIgnoreCase(String bookingName);

      boolean existsByUserIdAndCourseIdAndBookingStatusNotIn(
                  Integer userId,
                  Integer courseId,
                  Collection<String> bookingStatuses
            );

}
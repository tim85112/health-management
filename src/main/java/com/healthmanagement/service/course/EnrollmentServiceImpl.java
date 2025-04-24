package com.healthmanagement.service.course;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest; // 引入 PageRequest
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.healthmanagement.dao.course.CourseDAO;
import com.healthmanagement.dao.course.EnrollmentDAO;
import com.healthmanagement.dao.course.TrialBookingDAO; // 引入 TrialBookingDAO
import com.healthmanagement.dao.member.UserDAO;
import com.healthmanagement.dto.course.EnrollmentDTO;
import com.healthmanagement.dto.course.EnrollmentStatusUpdateDTO;
import com.healthmanagement.dto.course.CourseInfoDTO;
import com.healthmanagement.model.course.Course;
import com.healthmanagement.model.course.Enrollment;
import com.healthmanagement.model.course.TrialBooking; // 引入 TrialBooking Entity
import com.healthmanagement.model.member.User;

import com.healthmanagement.dto.course.ConvertToDTO;

import jakarta.persistence.EntityNotFoundException;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.temporal.ChronoUnit;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.Collection; // 確保引入 Collection

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Service
public class EnrollmentServiceImpl implements EnrollmentService {

    private static final Logger logger = LoggerFactory.getLogger(EnrollmentServiceImpl.class);

    @Autowired
    private EnrollmentDAO enrollmentDAO;

    @Autowired
    private UserDAO userDAO;

    @Autowired
    private CourseDAO courseDAO;

    @Autowired
    private TrialBookingDAO trialBookingDAO; // 注入 TrialBookingDAO

    @Autowired
    private ConvertToDTO convertToDTOConverter;

    private static final String REGISTERED_STATUS = "已報名";
    private static final String CANCELLED_STATUS = "已取消";
    private static final String WAITING_STATUS = "候補中";
    private static final String COMPLETED_STATUS = "已完成";
    private static final String NO_SHOW_STATUS = "未到場";

    // 活躍的狀態列表
    private static final List<String> ACTIVE_ENROLLMENT_STATUSES = List.of(REGISTERED_STATUS, WAITING_STATUS);
    private static final List<String> ACTIVE_TRIAL_STATUSES = List.of("已預約");

    // 非活躍狀態列表（用於查詢時排除）
    private static final List<String> INACTIVE_ENROLLMENT_STATUSES = List.of(CANCELLED_STATUS, COMPLETED_STATUS, NO_SHOW_STATUS);
    private static final List<String> INACTIVE_TRIAL_STATUSES = List.of(CANCELLED_STATUS, COMPLETED_STATUS, NO_SHOW_STATUS);


    // 設定報名和取消檢查的時限（例如提前 24 小時）
    private static final long ENROLLMENT_CANCEL_CUTOFF_HOURS = 24;

    // 計算課程的下一次發生時間，相對於當前日期時間
    // 複製自 CourseServiceImpl，並稍作調整 (假設 DayOfWeek 是 0=Sun, 6=Sat)
    private LocalDateTime calculateNextCourseOccurrenceTime(Course course) {
         if (course.getDayOfWeek() == null || course.getStartTime() == null) {
              logger.warn("Course ID {} has incomplete scheduling info (dayOfWeek: {}, startTime: {}). Cannot calculate next occurrence.",
                          course.getId(), course.getDayOfWeek(), course.getStartTime());
              return null;
         }
         LocalDate today = LocalDate.now();
         LocalTime nowTime = LocalTime.now();
         // 將資料庫的 0-6 (Sun-Sat) 轉換為 Java 的 DayOfWeek (1-7, Mon-Sun)
         // 或者根據你的資料庫實際定義調整轉換邏輯
         int dbDayOfWeek = course.getDayOfWeek(); // 假設資料庫是 0-6 (Sun-Sat)
         DayOfWeek courseDayOfWeek = DayOfWeek.of((dbDayOfWeek + 1) % 7 == 0 ? 7 : (dbDayOfWeek + 1) % 7); // 轉換為 1-7 (Mon-Sun)
         DayOfWeek todayDayOfWeek = today.getDayOfWeek(); // Java 的 DayOfWeek (1-7, Mon-Sun)


         LocalDate nextDate = today;
         int daysUntilNext = courseDayOfWeek.getValue() - todayDayOfWeek.getValue();

         if (daysUntilNext < 0) {
             daysUntilNext += 7; // 課程日在本週已經過了，下一次是在下週
         }
         // 如果課程日是今天，但課程開始時間已經過了當前時間，下一次也是在下週
         if (daysUntilNext == 0 && course.getStartTime().isBefore(nowTime)) {
             daysUntilNext = 7;
         }
         nextDate = today.plusDays(daysUntilNext);
         return LocalDateTime.of(nextDate, course.getStartTime());
    }

    // 檢查課程的下一次發生時間是否在指定的時限內
    private boolean isWithinHours(Course course, long hours) {
        LocalDateTime nextCourseTime = calculateNextCourseOccurrenceTime(course);
        if (nextCourseTime == null) {
             // 如果無法計算時間，視為在時限內（阻止報名/取消）
             // 也可以選擇拋出例外
             logger.warn("Cannot calculate next occurrence time for Course ID {}. Assuming within cutoff hours.", course.getId());
             return true;
        }
        LocalDateTime now = LocalDateTime.now();
        // 檢查是否在未來，並且距離小於指定小時數
        // calculateNextCourseOccurrenceTime 已經確保是未來的時間點
        return nextCourseTime.isAfter(now) && ChronoUnit.HOURS.between(now, nextCourseTime) < hours;
    }


    // 處理檢查是否已報名/候補、課程是否已滿、以及更新已取消記錄。
    @Transactional
    private Enrollment performEnrollmentLogic(User user, Course course) {
        logger.info("執行核心報名邏輯，使用者 ID: {}，課程 ID: {}", user.getId(), course.getId());

        // 檢查使用者是否已經有常規報名記錄（狀態不在非活躍列表中的）
        // 使用 EnrollmentDAO 中現有的 existsByUserAndCourseAndStatusNotIn 方法
        if (enrollmentDAO.existsByUserAndCourseAndStatusNotIn(user, course, INACTIVE_ENROLLMENT_STATUSES)) {
             logger.warn("使用者 ID {} 已有課程 ID {} 的活躍常規報名記錄。", user.getId(), course.getId());
             // 進一步判斷具體狀態，給予更精確的錯誤訊息
             if (enrollmentDAO.existsByUserAndCourseAndStatus(user, course, REGISTERED_STATUS)) {
                 throw new IllegalStateException("您已報名此課程");
             } else if (enrollmentDAO.existsByUserAndCourseAndStatus(user, course, WAITING_STATUS)) {
                 throw new IllegalStateException("您已在候補名單中");
             } else {
                 // 如果存在其他活躍狀態（如果你的系統有更多狀態）
                 throw new IllegalStateException("您已存在其他活躍狀態的報名記錄於此課程。");
             }
        }
        logger.info("使用者 ID {} 於課程 ID {} 不存在活躍常規報名記錄。", user.getId(), course.getId());

        // 檢查課程是否已滿 (優先判斷常規報名人數是否達到 maxCapacity)
        if (isCourseFull(course.getId())) {
            logger.info("課程 ID {} 已滿，將使用者 ID {} 加入候補名單。", course.getId(), user.getId());
            // 課程已滿，直接加入候補名單 (不論之前是否有取消記錄)
            Enrollment waitlistItem = Enrollment.builder()
                    .user(user)
                    .course(course)
                    .enrollmentTime(LocalDateTime.now())
                    .status(WAITING_STATUS) // 設定為候補中
                    .build();
            return enrollmentDAO.save(waitlistItem);
        } else {
            logger.info("課程 ID {} 未滿，將嘗試直接報名使用者 ID {}。", course.getId(), user.getId());
            // 課程未滿，檢查是否有之前的「已取消」記錄，以決定是更新還是新建
            Optional<Enrollment> existingCancelledEnrollmentOpt = enrollmentDAO.findByUserAndCourseAndStatus(user, course, CANCELLED_STATUS);
            if (existingCancelledEnrollmentOpt.isPresent()) {
                 logger.info("找到使用者 ID {} 於課程 ID {} 的已取消報名記錄 ID {}，將更新狀態。",
                             user.getId(), course.getId(), existingCancelledEnrollmentOpt.get().getId());
                 // 找到「已取消」記錄，更新這筆記錄為「已報名」
                 Enrollment existingCancelledEnrollment = existingCancelledEnrollmentOpt.get();
                 existingCancelledEnrollment.setEnrollmentTime(LocalDateTime.now()); // 更新報名時間為重新報名的時間
                 existingCancelledEnrollment.setStatus(REGISTERED_STATUS); // 改為已報名
                 return enrollmentDAO.save(existingCancelledEnrollment);
            } else {
                 logger.info("使用者 ID {} 於課程 ID {} 沒有已取消記錄，創建新的已報名記錄。", user.getId(), course.getId());
                 // 課程未滿，且沒有「已取消」記錄，創建新的「已報名」記錄
                 Enrollment newEnrollment = Enrollment.builder()
                         .user(user)
                         .course(course)
                         .enrollmentTime(LocalDateTime.now())
                         .status(REGISTERED_STATUS) // 設定為已報名
                         .build();
                 return enrollmentDAO.save(newEnrollment);
            }
        }
    }

    // 報名常規課程（包含 24 小時限制）
    @Transactional
    @Override
    public EnrollmentDTO enrollUserToCourse(Integer userId, Integer courseId) {
        logger.info("使用者 ID {} 嘗試報名常規課程 ID: {}", userId, courseId);
        User user = userDAO.findById(userId)
                .orElseThrow(() -> new EntityNotFoundException("找不到使用者 ID: " + userId));
        Course course = courseDAO.findById(courseId)
                .orElseThrow(() -> new EntityNotFoundException("找不到課程 ID: " + courseId));

        // 移除對 offersTrialOption 的檢查，因為常規報名邏輯適用於所有提供常規報名的課程

        // 檢查課程的下一次發生時間是否在 24 小時內開始 (報名截止時間)
        if (isWithinHours(course, ENROLLMENT_CANCEL_CUTOFF_HOURS)) {
            LocalDateTime nextCourseTime = calculateNextCourseOccurrenceTime(course);
            logger.warn("使用者 ID {} 報名課程 ID {} 失敗：課程將於 {} 開始，距離不足 {} 小時。",
                       userId, courseId, nextCourseTime, ENROLLMENT_CANCEL_CUTOFF_HOURS);
            throw new IllegalStateException(String.format("報名失敗：課程將於 %s 開始，距離不足 %d 小時。",
                    nextCourseTime != null ? nextCourseTime.toString() : "未知時間", ENROLLMENT_CANCEL_CUTOFF_HOURS));
        }
        logger.info("確認課程 ID {} 報名時間符合提前預約時限 (提前 {} 小時)", courseId, ENROLLMENT_CANCEL_CUTOFF_HOURS);

        Enrollment resultEnrollment = performEnrollmentLogic(user, course);
        return convertToDTOConverter.convertToEnrollmentDTO(resultEnrollment);
    }

    // 取消常規課程報名（包含 24 小時取消限制和候補自動遞補邏輯）
    @Transactional
    @Override
    public void cancelEnrollment(Integer enrollmentId) {
        logger.info("嘗試取消常規報名 ID: {}", enrollmentId);
        Enrollment enrollment = enrollmentDAO.findById(enrollmentId)
                .orElseThrow(() -> new EntityNotFoundException("找不到報名 ID: + enrollmentId"));
        Course course = enrollment.getCourse();
        if (course == null) {
            logger.error("報名 ID {} 關聯的課程為 null，無法檢查取消時限。", enrollmentId);
            throw new IllegalStateException("報名記錄關聯的課程無效，無法取消。");
        }

        // 移除對 offersTrialOption 的檢查，因為取消邏輯適用於所有常規報名

        // 檢查課程的下一次發生時間是否在 24 小時內開始 (取消截止時間)
        if (isWithinHours(course, ENROLLMENT_CANCEL_CUTOFF_HOURS)) {
            LocalDateTime nextCourseTime = calculateNextCourseOccurrenceTime(course);
            logger.warn("取消報名 ID {} 失敗：課程將於 {} 開始，距離不足 {} 小時。",
                       enrollmentId, nextCourseTime, ENROLLMENT_CANCEL_CUTOFF_HOURS);
             // 使用 IllegalStateException 更通用，或者自定義 Exception
            throw new IllegalStateException(String.format("取消失敗：課程將於 %s 開始，距離不足 %d 小時。",
                    nextCourseTime != null ? nextCourseTime.toString() : "未知時間", ENROLLMENT_CANCEL_CUTOFF_HOURS));
        }
        logger.info("確認報名 ID {} 取消時間符合提前取消時限 (提前 {} 小時)", enrollmentId, ENROLLMENT_CANCEL_CUTOFF_HOURS);

        // 檢查取消的記錄狀態，只有已報名或候補中等活躍狀態才能被取消
        // INACTIVE_ENROLLMENT_STATUSES 列表包含了不能取消的狀態
        if (INACTIVE_ENROLLMENT_STATUSES.contains(enrollment.getStatus())) {
            logger.warn("報名 ID {} 狀態 {} 不允許取消。", enrollmentId, enrollment.getStatus());
            throw new IllegalStateException(String.format("報名狀態不正確，無法取消。目前狀態: %s", enrollment.getStatus()));
        }

        // 檢查取消的是否是 '已報名' 記錄，以便後續觸發候補遞補
        boolean wasRegistered = REGISTERED_STATUS.equals(enrollment.getStatus());
        logger.info("報名 ID {} 原狀態為 {}", enrollmentId, enrollment.getStatus());

        // 執行取消操作：將狀態設為 '已取消'
        enrollment.setStatus(CANCELLED_STATUS);
        enrollmentDAO.save(enrollment);
        logger.info("報名 ID {} 狀態更新為 {}。", enrollmentId, CANCELLED_STATUS);

        // 候補名單自動遞補
        // 只有當取消的是 '已報名' 的記錄時，且課程仍有空位時才觸發遞補
        if (wasRegistered) {
            // 檢查課程是否已滿，未滿才觸發候補遞補
             if (!isCourseFull(course.getId())) {
                processWaitlist(course.getId());
             } else {
                 logger.info("課程 ID {} 仍滿，取消報名 ID {} 未觸發候補遞補。", course.getId(), enrollmentId);
             }
        }
    }

    // 查詢並返回指定使用者的所有常規課程報名記錄
    @Override
    public List<EnrollmentDTO> getEnrollmentsByUserId(Integer userId) {
        logger.info("查詢使用者 ID {} 的所有常規報名記錄...", userId);
        User user = userDAO.findById(userId)
                .orElseThrow(() -> new EntityNotFoundException("User not found with id: " + userId));
        // 這裡不需要根據 offersTrialOption 過濾，因為 Enrollment 記錄本身就是常規報名記錄
        List<Enrollment> userEnrollments = enrollmentDAO.findByUser(user);
        logger.info("找到使用者 ID {} 的 {} 條報名記錄。", userId, userEnrollments.size());

        return userEnrollments.stream()
                .map(convertToDTOConverter::convertToEnrollmentDTO)
                .collect(Collectors.toList());
    }

    // 查詢並返回特定常規課程的所有報名記錄
    @Override
    public List<EnrollmentDTO> getEnrollmentsByCourseId(Integer courseId) {
        logger.info("查詢常規課程 ID {} 的所有報名記錄...", courseId);
        Course course = courseDAO.findById(courseId)
                .orElseThrow(() -> new EntityNotFoundException("Course not found with id: " + courseId));

        // 移除對 offersTrialOption 的檢查，一個課程即使提供體驗選項，也可能有常規報名記錄

        List<Enrollment> courseEnrollments = enrollmentDAO.findByCourse(course);
        logger.info("找到課程 ID {} 的 {} 條常規報名記錄。", courseId, courseEnrollments.size());
        return courseEnrollments.stream()
                .map(convertToDTOConverter::convertToEnrollmentDTO)
                .collect(Collectors.toList());
    }

    // 檢查特定常規課程是否已達到最大報名容量（基於已報名人數）
    @Override
    public boolean isCourseFull(Integer courseId) {
        logger.info("檢查常規課程 ID {} 是否已滿...", courseId);
        Course course = courseDAO.findById(courseId)
                .orElseThrow(() -> new EntityNotFoundException("Course not found with id: " + courseId));
        // 移除對 offersTrialOption 的檢查，CourseFull 的判斷只基於 maxCapacity 和 常規報名人數

        int enrolledCount = enrollmentDAO.countByCourseAndStatus(course, REGISTERED_STATUS);
        boolean isFull = enrolledCount >= course.getMaxCapacity();
        logger.info("常規課程 ID {} 已報名人數: {}，最大容量: {}，是否已滿: {}",
                   courseId, enrolledCount, course.getMaxCapacity(), isFull);
        return isFull;
    }

    // 檢查指定使用者是否已對特定常規課程進行有效報名或候補 (狀態不在非活躍列表中的)
    @Override
    public boolean isUserEnrolled(Integer userId, Integer courseId) {
        logger.info("檢查使用者 ID {} 是否已有效報名或候補常規課程 ID: {}", userId, courseId);
        User user = userDAO.findById(userId)
                .orElseThrow(() -> new EntityNotFoundException("User not found with id: " + userId));
        Course course = courseDAO.findById(courseId)
                .orElseThrow(() -> new EntityNotFoundException("Course not found with id: " + courseId));
        // 移除對 offersTrialOption 的檢查，isUserEnrolled 判斷只關乎常規報名表 Enrollment

        // 使用 EnrollmentDAO 中更新後的方法，檢查是否存在狀態不在非活躍列表中的記錄
        boolean isEnrolled = enrollmentDAO.existsByUserAndCourseAndStatusNotIn(user, course, INACTIVE_ENROLLMENT_STATUSES);
        logger.info("使用者 ID {} 是否已有效報名或候補常規課程 ID {}: {}", userId, courseId, isEnrolled);
        return isEnrolled;
    }

    // 查詢並返回特定常規課程當前處於 已報名 狀態的人數
    @Override
    public int getEnrolledCount(Integer courseId) {
        logger.info("查詢常規課程 ID {} 的已報名人數...", courseId);
        Course course = courseDAO.findById(courseId)
                .orElseThrow(() -> new EntityNotFoundException("Course not found with id: " + courseId));

        // 移除對 offersTrialOption 的檢查，已報名人數的計數只關乎常規報名表 Enrollment

        int count = enrollmentDAO.countByCourseAndStatus(course, REGISTERED_STATUS);
        logger.info("常規課程 ID {} 的已報名人數: {}", courseId, count);
        return count;
    }

    // 檢查特定課程是否存在活躍的常規報名記錄 (新增用於 CourseService 刪除檢查)
    @Override
    public boolean hasActiveEnrollmentsForCourse(Integer courseId) {
        logger.info("檢查課程 ID {} 是否存在活躍的常規報名記錄...", courseId);
        // 使用 EnrollmentDAO 中新添加的方法，檢查是否存在狀態不在非活躍列表中的記錄
        boolean hasActive = enrollmentDAO.existsByCourseIdAndStatusNotIn(courseId, INACTIVE_ENROLLMENT_STATUSES);
        logger.info("課程 ID {} 是否存在活躍的常規報名記錄: {}", courseId, hasActive);
        return hasActive;
    }


 // 查詢所有課程並包含當前使用者的報名狀態和人數 (常規和體驗)
    // 這個方法被大幅修改以整合常規和體驗資訊
    @Override
    public List<CourseInfoDTO> getAllCoursesWithUserStatus(Integer userId) {
        logger.info("查詢所有課程並包含使用者 {} 的常規報名和體驗預約狀態及人數...", userId != null ? userId : "匿名");

        // 1. 獲取所有課程
        List<Course> allCourses = courseDAO.findAll();
        if (allCourses.isEmpty()) {
            logger.info("沒有找到任何課程。");
            return Collections.emptyList();
        }
        logger.info("找到 {} 條課程記錄。", allCourses.size());

        final User finalUser = (userId != null) ? userDAO.findById(userId).orElse(null) : null;
        if (userId != null && finalUser == null) {
            logger.warn("傳入的使用者 ID {} 未找到對應的 User 實體，將無法查詢個人狀態。", userId);
        }

        // 2. 獲取常規報名相關數據 (已報名和候補人數，以及當前使用者的常規報名狀態)
        // 獲取所有課程中已報名和候補的記錄
        List<Enrollment> allRegisteredEnrollments = enrollmentDAO.findByStatus(REGISTERED_STATUS); // 查找所有已報名記錄
        List<Enrollment> allWaitingEnrollments = enrollmentDAO.findByStatus(WAITING_STATUS); // 查找所有候補中記錄

        // 將常規報名記錄按課程 ID 分組計數
        final Map<Integer, Long> registeredCountsMap = allRegisteredEnrollments.stream()
                .filter(e -> e.getCourse() != null)
                .collect(Collectors.groupingBy(e -> e.getCourse().getId(), Collectors.counting()));

        final Map<Integer, Long> waitlistCountsMap = allWaitingEnrollments.stream()
                .filter(e -> e.getCourse() != null)
                .collect(Collectors.groupingBy(e -> e.getCourse().getId(), Collectors.counting()));

        // 如果有登入使用者，獲取其所有活躍常規報名記錄 (狀態不在非活躍列表中的)
        // 使用 EnrollmentDAO 中新添加的 findByUserAndStatusNotIn 方法
        final Map<Integer, Enrollment> userActiveEnrollmentsMap = (finalUser != null) ?
                enrollmentDAO.findByUserAndStatusNotIn(finalUser, INACTIVE_ENROLLMENT_STATUSES).stream()
                        .filter(e -> e.getCourse() != null)
                        .collect(Collectors.toMap(e -> e.getCourse().getId(), Function.identity())) :
                Collections.emptyMap();


        // 3. 獲取體驗預約相關數據 (下一個排程的已預約體驗人數，以及當前使用者的體驗預約狀態)
        // 獲取所有提供體驗選項的課程
        List<Course> coursesOfferingTrial = allCourses.stream()
                .filter(course -> course.getOffersTrialOption() != null && course.getOffersTrialOption())
                .collect(Collectors.toList());

        // 計算這些課程的下一個排程日期和時間
        Map<Integer, LocalDateTime> nextOccurrenceTimes = coursesOfferingTrial.stream()
                .collect(Collectors.toMap(
                        Course::getId,
                        this::calculateNextCourseOccurrenceTime // 使用輔助方法計算下一次排程時間
                ));

        // 獲取需要查詢體驗預約計數的課程 ID 列表 (確保下一個排程時間不為 null)
        List<Integer> courseIdsForTrialCountQuery = nextOccurrenceTimes.entrySet().stream()
                .filter(entry -> entry.getValue() != null)
                .map(Map.Entry::getKey)
                .collect(Collectors.toList());

        // 批量獲取這些課程的下一個排程的已預約體驗人數
        Map<Integer, Integer> bookedTrialCountsMap = new java.util.HashMap<>(); // 初始化為空 Map
        if (!courseIdsForTrialCountQuery.isEmpty()) {
             // 遍歷調用單個計數方法 (使用 TrialBookingDAO 中已有的 countTrialBookingsByCourseDateActualStartTimeAndStatusNotInNative)
             for (Integer courseId : courseIdsForTrialCountQuery) {
                 LocalDateTime nextTime = nextOccurrenceTimes.get(courseId);
                 if (nextTime != null) { // 再次確認，以防萬一
                     int count = trialBookingDAO.countTrialBookingsByCourseDateActualStartTimeAndStatusNotInNative(
                         courseId,
                         nextTime.toLocalDate(),
                         nextTime.toLocalTime(), // 注意這裡需要 LocalTime
                         INACTIVE_TRIAL_STATUSES
                     );
                     bookedTrialCountsMap.put(courseId, count);
                 }
             }
             logger.info("獲取了 {} 個提供體驗選項課程的下一個排程體驗預約計數。", bookedTrialCountsMap.size());
        } else {
            logger.info("沒有提供體驗選項的課程，或者無法計算下一個排程時間，跳過體驗預約計數查詢。");
        }

     // 如果有登入使用者，獲取其對這些提供體驗選項課程的活躍體驗預約記錄
     // 使用 TrialBookingDAO 中能夠批量查詢使用者活躍體驗預約的方法 (findByUserAndCourseIdInAndBookingStatusNotInAndBookingDateGreaterThanEqual)
     // 修正 effectively final 錯誤，將變數在 if/else 中都初始化
     final Map<Integer, TrialBooking> userActiveTrialBookingsMap; // 宣告為 final

     if (finalUser != null && !coursesOfferingTrial.isEmpty()) {
         // MODIFICATION: 從課程列表中提取 ID 列表
         List<Integer> courseIdsForUserTrialQuery = coursesOfferingTrial.stream()
                                                       .map(Course::getId) // 提取課程 ID
                                                       .collect(Collectors.toList());
         // 只有在有課程需要查詢時才執行 DAO 查詢
         if (!courseIdsForUserTrialQuery.isEmpty()) {
             // 調用新的 DAO 方法
             List<TrialBooking> userBookingsForCourses = trialBookingDAO.findByUserAndCourseIdInAndBookingStatusNotInAndBookingDateGreaterThanEqual(
                 finalUser,
                 courseIdsForUserTrialQuery, // 傳入課程 ID 列表
                 INACTIVE_TRIAL_STATUSES,
                 LocalDate.now() // 查詢從今天開始的預約
             );
              // 在 if 塊中初始化 final 變數
             userActiveTrialBookingsMap = userBookingsForCourses.stream()
                  .filter(tb -> tb.getCourse() != null) // 確保 course 不為 null
                  .collect(Collectors.toMap(tb -> tb.getCourse().getId(), Function.identity()));

             logger.info("找到使用者 {} 對 {} 個課程的活躍體驗預約記錄。", userId, userActiveTrialBookingsMap.size());
         } else {
             // 如果 coursesOfferingTrial 是空的，courseIdsForUserTrialQuery 也是空的，userBookingsForCourses 會是空列表，這裡可以簡單初始化為空 Map
             userActiveTrialBookingsMap = Collections.emptyMap();
             logger.info("使用者 {} 沒有提供體驗選項的課程，跳過體驗預約記錄查詢。", userId);
         }

     } else {
          // 在 else 塊中初始化 final 變數為空 Map
          userActiveTrialBookingsMap = Collections.emptyMap();
          if (finalUser != null) { // 登入使用者但沒有體驗課
              logger.info("使用者 {} 沒有活躍體驗預約記錄。", userId);
          } else { // 未登入使用者
              logger.info("使用者未登入，跳過體驗預約記錄查詢。", userId);
          }
     }


        // 4. 將所有數據組裝到 CourseInfoDTO 列表中
        return allCourses.stream().map(course -> {
            // 常規報名數據
            long registeredCount = registeredCountsMap.getOrDefault(course.getId(), 0L);
            long waitlistCount = waitlistCountsMap.getOrDefault(course.getId(), 0L);

            // 使用者常規報名狀態和 ID
            String userStatus = "未報名"; // 預設狀態
            Integer userEnrollmentId = null;
            if (finalUser != null) {
                 Enrollment currentUserEnrollment = userActiveEnrollmentsMap.get(course.getId());
                 if (currentUserEnrollment != null) {
                      userStatus = currentUserEnrollment.getStatus();
                      userEnrollmentId = currentUserEnrollment.getId();
                 }
            }

            // 體驗預約數據 (只針對提供體驗選項的課程)
            Boolean offersTrialOption = course.getOffersTrialOption() != null ? course.getOffersTrialOption() : false;
            Integer maxTrialCapacity = course.getMaxTrialCapacity();
            // 從 Map 中獲取體驗預約計數，如果課程不提供體驗或無法計算時間，則為 0
            Integer bookedTrialCount = bookedTrialCountsMap.getOrDefault(course.getId(), 0);

            // 使用者體驗預約狀態和 ID
            String userTrialBookingStatus = "未預約"; // 預設狀態
            Integer userTrialBookingId = null;
            // 只有登入用戶且課程提供體驗選項才檢查體驗預約狀態
            // 這裡使用 final 的 userActiveTrialBookingsMap
            if (finalUser != null && offersTrialOption) {
                 TrialBooking currentUserTrialBooking = userActiveTrialBookingsMap.get(course.getId());
                 if (currentUserTrialBooking != null) {
                      userTrialBookingStatus = currentUserTrialBooking.getBookingStatus(); // 例如 "已預約"
                      userTrialBookingId = currentUserTrialBooking.getId();
                 }
            }


            return CourseInfoDTO.builder()
                    .id(course.getId())
                    .name(course.getName())
                    .description(course.getDescription())
                    .dayOfWeek(course.getDayOfWeek())
                    .startTime(course.getStartTime())
                    .duration(course.getDuration())
                    .maxCapacity(course.getMaxCapacity())
                    // 常規報名數據
                    .registeredCount((int) registeredCount)
                    .waitlistCount((int) waitlistCount)
                    // 使用者常規報名狀態和 ID
                    .userStatus(userStatus)
                    .userEnrollmentId(userEnrollmentId)
                    // 體驗預約數據
                    .offersTrialOption(offersTrialOption)
                    .maxTrialCapacity(maxTrialCapacity)
                    .bookedTrialCount(bookedTrialCount)
                    // 使用者體驗預約狀態和 ID
                    .userTrialBookingStatus(userTrialBookingStatus)
                    .userTrialBookingId(userTrialBookingId)
                    .build();
        }).collect(Collectors.toList());
    }


    // 將指定使用者加入特定常規課程的候補名單
    @Transactional
    @Override
    public EnrollmentDTO addWaitlistItem(Integer userId, Integer courseId) {
        logger.info("嘗試將使用者 ID {} 加入課程 ID {} 的候補名單...", userId, courseId);
        User user = userDAO.findById(userId)
                .orElseThrow(() -> new EntityNotFoundException("User not found with id: " + userId));
        Course course = courseDAO.findById(courseId)
                .orElseThrow(() -> new EntityNotFoundException("Course not found with id: " + courseId));

        // 移除對 offersTrialOption 的檢查
        // 所有提供常規報名的課程都可以有候補名單

        logger.info("課程 ID {} 接受常規報名，繼續候補邏輯。", courseId);

        // 檢查使用者是否已經有常規報名記錄（狀態不在非活躍列表中的）
        // 使用 EnrollmentDAO 中現有的 existsByUserAndCourseAndStatusNotIn 方法
        if (enrollmentDAO.existsByUserAndCourseAndStatusNotIn(user, course, INACTIVE_ENROLLMENT_STATUSES)) {
             logger.warn("使用者 ID {} 已有課程 ID {} 的活躍常規報名記錄。", user.getId(), course.getId());
             // 進一步判斷具體狀態，給予更精確的錯誤訊息
             if (enrollmentDAO.existsByUserAndCourseAndStatus(user, course, REGISTERED_STATUS)) {
                 throw new IllegalStateException("您已報名此課程");
             } else if (enrollmentDAO.existsByUserAndCourseAndStatus(user, course, WAITING_STATUS)) {
                 throw new IllegalStateException("您已在候補名單中");
             } else {
                 // 如果存在其他活躍狀態（如果你的系統有更多狀態）
                 throw new IllegalStateException("您已存在其他活躍狀態的報名記錄於此課程。");
             }
        }
        logger.info("使用者 ID {} 於課程 ID {} 不存在活躍常規報名記錄。", user.getId(), course.getId());

        // 檢查課程是否已滿 (必須已滿才能加入候補)
        if (!isCourseFull(course.getId())) {
             logger.warn("嘗試將使用者 ID {} 加入課程 ID {} 的候補名單，但課程未滿。", user.getId(), courseId);
             throw new IllegalStateException("課程未滿，可以直接報名。");
        }

        Enrollment waitlistItem = Enrollment.builder()
                .user(user)
                .course(course)
                .enrollmentTime(LocalDateTime.now())
                .status(WAITING_STATUS)
                .build();
        Enrollment savedWaitlistItem = enrollmentDAO.save(waitlistItem);
        logger.info("使用者 ID {} 成功加入課程 ID {} 的候補名單，報名 ID: {}",
                   user.getId(), course.getId(), savedWaitlistItem.getId());
        return convertToDTOConverter.convertToEnrollmentDTO(savedWaitlistItem);
    }

    // 處理特定常規課程的候補名單自動遞補
    @Transactional
    @Override
    public void processWaitlist(Integer courseId) {
        logger.info("處理常規課程 ID {} 的候補名單自動遞補...", courseId);
        Course course = courseDAO.findById(courseId)
                .orElseThrow(() -> new EntityNotFoundException("Course not found with id: " + courseId));

        // 移除對 offersTrialOption 的檢查
        // 候補遞補邏輯適用於所有提供常規報名的課程

        logger.info("課程 ID {} 接受常規報名，進行候補遞補處理。", courseId);


        long registeredCount = enrollmentDAO.countByCourseAndStatus(course, REGISTERED_STATUS);
        int maxCapacity = course.getMaxCapacity();

        if (registeredCount < maxCapacity) {
            logger.info("課程 ID {} 有空位，已報名人數: {}，最大容量: {}", course.getId(), registeredCount, maxCapacity);
            // 按報名時間升序獲取候補中的記錄
            List<Enrollment> waitingList = enrollmentDAO.findByCourseAndStatusOrderByEnrollmentTimeAsc(course, WAITING_STATUS);

            if (waitingList.isEmpty()) {
                logger.info("課程 ID {} 候補名單為空，無需遞補。", course.getId());
                return;
            }
            logger.info("課程 ID {} 找到 {} 位候補中的使用者。", course.getId(), waitingList.size());

            int slotsAvailable = (int) (maxCapacity - registeredCount);
            int promotedCount = 0;

            for (int i = 0; i < Math.min(slotsAvailable, waitingList.size()); i++) {
                Enrollment enrollment = waitingList.get(i);
                // 確保只處理狀態仍為 WAITING_STATUS 的記錄，以防手動修改狀態
                 if (WAITING_STATUS.equals(enrollment.getStatus())) {
                     enrollment.setStatus(REGISTERED_STATUS);
                     enrollmentDAO.save(enrollment);
                     promotedCount++;
                     logger.info("候補名單自動遞補：使用者 {} (報名 ID {}) 已從候補遞補為已報名 課程 ID: {}",
                             enrollment.getUser() != null ? enrollment.getUser().getId() : "Unknown User",
                             enrollment.getId(), course.getId());
                     // *** 可選：發送通知給被遞補的使用者 (需要額外的通知服務) ***
                     // 例如：notificationService.sendPromotionNotification(promotedEnrollment.getUser(), course);
                 } else {
                      logger.warn("候補報名 ID {} 狀態 {} 不正確，跳過遞補。", enrollment.getId(), enrollment.getStatus());
                 }
            }
            logger.info("課程 ID {} 候補名單自動遞補完成，遞補了 {} 位使用者。", course.getId(), promotedCount);

        } else {
            logger.info("課程 ID {} 已滿，已報名人數: {}，最大容量: {}，無需遞補。", course.getId(), registeredCount, maxCapacity);
        }
    }

    // 手動將指定報名記錄的狀態更新為任何給定的狀態字串。
    @Transactional
    @Override
    public EnrollmentDTO updateEnrollmentStatus(Integer enrollmentId, EnrollmentStatusUpdateDTO updateDTO) {
        String newStatus = updateDTO.getStatus();
        logger.info("嘗試手動更新報名 ID {} 的狀態為 {}", enrollmentId, newStatus);
        Enrollment enrollment = enrollmentDAO.findById(enrollmentId)
                .orElseThrow(() -> new EntityNotFoundException("找不到報名 ID: " + enrollmentId));
        String oldStatus = enrollment.getStatus();

        // 驗證新狀態是否有效，並檢查狀態轉換規則 (如果需要)
        // 這裡簡單驗證了不允許轉換為當前狀態，以及不允許從非活躍狀態轉換
        if (oldStatus.equals(newStatus)) {
             logger.warn("報名 ID {} 狀態已為 {}，無需更新。", enrollmentId, newStatus);
             return convertToDTOConverter.convertToEnrollmentDTO(enrollment); // 返回原 DTO
        }
        if (INACTIVE_ENROLLMENT_STATUSES.contains(oldStatus)) {
             logger.warn("無法從目前狀態 '{}' 更改報名 ID {} 的狀態。", oldStatus, enrollmentId);
             throw new IllegalStateException(String.format("無法從目前狀態 '%s' 更改報名狀態。", oldStatus));
        }
        // TODO: 可選：更嚴格的狀態轉換規則驗證 (例如，只能從 WAITING 轉為 REGISTERED)

        enrollment.setStatus(newStatus);
        Enrollment updatedEnrollmentEntity = enrollmentDAO.save(enrollment);
        logger.info("報名 ID {} 狀態成功從 {} 更新為 {}", enrollmentId, oldStatus, newStatus);

        // 如果狀態從活躍變為已取消，觸發候補遞補
        boolean wasActive = ACTIVE_ENROLLMENT_STATUSES.contains(oldStatus); // 使用活躍狀態列表
        boolean isNowCancelled = CANCELLED_STATUS.equals(newStatus);

        if (wasActive && isNowCancelled) {
            logger.info("報名 ID {} 狀態從活躍 ({}) 變為已取消，觸發候補遞補流程。", enrollmentId, oldStatus);
            Course course = updatedEnrollmentEntity.getCourse();
            if (course != null) {
                 // 檢查課程是否已滿，未滿才觸發候補遞補
                 if (!isCourseFull(course.getId())) {
                    processWaitlist(course.getId());
                 } else {
                     logger.info("課程 ID {} 仍滿，手動取消報名 ID {} 未觸發候補遞補。", course.getId(), enrollmentId);
                 }
            } else {
                logger.warn("報名 ID {} 關聯的課程為 null，無法觸發候補遞補流程。", enrollmentId);
            }
        } else {
            logger.info("報名 ID {} 狀態從 {} 更新為 {}，無需觸發候補遞補流程。", enrollmentId, oldStatus, newStatus);
        }
        return convertToDTOConverter.convertToEnrollmentDTO(updatedEnrollmentEntity);
    }

    // 計算課程最近一次過去的發生時間，相對於給定的時間點
    private LocalDateTime calculateLastPastCourseOccurrenceTime(Course course, LocalDateTime relativeTo) {
         if (course.getDayOfWeek() == null || course.getStartTime() == null) {
              logger.warn("Course ID {} has incomplete scheduling info (dayOfWeek: {}, startTime: {}). Cannot calculate last occurrence.",
                          course.getId(), course.getDayOfWeek(), course.getStartTime());
              return null;
         }
         LocalDate relativeDate = relativeTo.toLocalDate();
         LocalTime relativeTime = relativeTo.toLocalTime();
         int dbDayOfWeek = course.getDayOfWeek(); // 假設資料庫是 0-6 (Sun-Sat)
         if (dbDayOfWeek < 0 || dbDayOfWeek > 6) {
             logger.warn("Course ID {} has invalid dayOfWeek value: {}. Cannot calculate last occurrence.", course.getId(), dbDayOfWeek);
             return null;
         }
         DayOfWeek courseDayOfWeek = DayOfWeek.of((dbDayOfWeek + 1) % 7 == 0 ? 7 : (dbDayOfWeek + 1) % 7); // 轉換為 1-7 (Mon-Sun)
         DayOfWeek relativeDayOfWeek = relativeTo.getDayOfWeek(); // Java 的 DayOfWeek (1-7, Mon-Sun)


         LocalDate lastDate = relativeDate;
         // 計算當前日期與課程星期幾之間的差距天數
         int daysSinceLast = relativeDayOfWeek.getValue() - courseDayOfWeek.getValue();

         if (daysSinceLast < 0) {
             daysSinceLast += 7; // 課程日在本週還沒到，最近一次是在上週
         }
         // 如果課程日是今天，但課程開始時間晚於相對時間點，最近一次發生是在上週
         if (daysSinceLast == 0 && course.getStartTime().isAfter(relativeTime)) {
             daysSinceLast = 7;
         }
         // 計算最近一次發生的日期
         lastDate = relativeDate.minusDays(daysSinceLast);
         return LocalDateTime.of(lastDate, course.getStartTime());
    }

    // 排程任務 - 處理過期的常規報名記錄（每天凌晨 2 點運行，將狀態為 '已報名' 且課程最近一次發生時間已過的記錄標記為 '未到場'）
    @Scheduled(cron = "0 0 2 * * ?")
    @Transactional
    public void processPastDueEnrollments() {
        logger.info("Running scheduled task: Processing past due enrollments...");
        LocalDateTime now = LocalDateTime.now();
        // 查找所有狀態為 '已報名' 的報名記錄
        // 這裡只查找 '已報名' 的記錄，因為候補中的記錄通常不需要被標記為未到場
        List<Enrollment> registeredEnrollments = enrollmentDAO.findByStatus(REGISTERED_STATUS);

        if (registeredEnrollments.isEmpty()) {
            logger.info("No registered enrollments found to process.");
            return;
        }
        logger.info("Found {} registered enrollments to process.", registeredEnrollments.size());

        int updatedCount = 0;
        for (Enrollment enrollment : registeredEnrollments) {
            Course course = enrollment.getCourse();
            if (course == null) {
                logger.error("Enrollment ID {} has null course. Skipping.", enrollment.getId());
                continue;
            }

            // 移除對 offersTrialOption 的檢查
            // 過期處理邏輯適用於所有常規報名，無論課程是否提供體驗選項

            // 計算該報名記錄所對應課程的最近一次過去的發生時間點
            LocalDateTime lastOccurrence = calculateLastPastCourseOccurrenceTime(course, now);
            // 如果計算出的最近一次發生時間存在，且當前時間在該時間點之後
            // 這表示學員應當已經參與了最近一次課程，但狀態仍是 '已報名'
            // 這裡假設如果最近一次課程已過，且未被手動標記為「已完成」，則標記為「未到場」。
            // （手動標記為「已完成」會使狀態不再是 REGISTERED_STATUS，因此不會被此方法查詢到）
            if (lastOccurrence != null && now.isAfter(lastOccurrence)) {
                // 標記為「未到場」
                enrollment.setStatus(NO_SHOW_STATUS);
                enrollmentDAO.save(enrollment);
                updatedCount++;
                logger.info("Marked Enrollment ID {} for Course {} as {}. Last occurrence: {}",
                            enrollment.getId(), course.getName(), NO_SHOW_STATUS, lastOccurrence);
            }
            // 註解：此邏輯簡單處理了定期重複的課程。如果課程有明確的開始/結束日期，
            // 則需要更複雜的邏輯來判斷該報名是否已處於過期狀態（即課程已結束且最近一次課程已過）。
        }
        logger.info("Finished processing past due enrollments. Updated: {}", updatedCount);
    }
    
 // 實現 getNextOccurrenceBookedTrialCounts 方法 - 目前是臨時實現
    @Override
    @Transactional(readOnly = true)
    public Map<Integer, Integer> getNextOccurrenceBookedTrialCounts(List<Course> courses) {
        logger.warn("EnrollmentService.getNextOccurrenceBookedTrialCounts 尚未實現具體邏輯，臨時返回空 Map。");

        // TODO: 在這裡實現真正的業務邏輯：
        // 這個方法的目標是計算 courses 列表中「每個課程」相對於「當前時間」的下一個排程日期時間點，
        // 然後查詢 TrialBookingDAO 獲取那個特定日期時間點的體驗預約人數。

        // 具體步驟可能包括：
        // 1. 創建一個空的 Map 來存放結果 (Map<Integer, Integer> bookedCountsMap = new HashMap<>();)
        // 2. 遍歷傳入的 List<Course> courses
        // 3. 對於每一個 Course，計算它相對於 LocalDateTime.now() 的下一個發生日期時間點。這需要類似 calculateNextCourseOccurrenceTime 的輔助方法。
        // 4. 如果成功計算出下一個發生時間點 (nextOccurrenceDateTime) 且不為 null：
        //    a. 使用 trialBookingDAO.countTrialBookingsByCourseDateActualStartTimeAndStatusNotInNative
        //       方法，傳入 course.getId()，nextOccurrenceDateTime.toLocalDate()，
        //       nextOccurrenceDateTime.toLocalTime()，以及 INACTIVE_TRIAL_STATUSES。
        //    b. 將返回的 count 放入 bookedCountsMap，鍵為 course.getId()。
        // 5. 返回 bookedCountsMap。

        // 臨時返回空 Map，讓程式碼可以編譯
        return Collections.emptyMap();
    }

    // 你可能需要一個輔助方法來計算下一個發生時間點，類似於 EnrollmentServiceImpl 中可能已經有的 calculateNextCourseOccurrenceTime
    // 如果 EnrollmentServiceImpl 中沒有這個方法，或者它不是 public 的，你可能需要將其添加到這裡。
    /*
     private LocalDateTime calculateNextCourseOccurrenceTime(Course course) {
         if (course == null || course.getDayOfWeek() == null || course.getStartTime() == null) {
             return null;
         }
         LocalDateTime now = LocalDateTime.now();
         LocalDate today = now.toLocalDate();

         DayOfWeek courseDayOfWeek;
         try {
             courseDayOfWeek = DayOfWeek.of(course.getDayOfWeek()); // 假設 1-7 對應 DayOfWeek
         } catch (Exception e) {
             logger.warn("課程 ID {} 的星期幾值無效: {}。", course.getId(), course.getDayOfWeek());
             return null;
         }

         LocalDate nextDate = today.with(TemporalAdjusters.nextOrSame(courseDayOfWeek));
         LocalDateTime nextOccurrence = LocalDateTime.of(nextDate, course.getStartTime());

         if (nextOccurrence.isBefore(now)) {
             nextOccurrence = nextOccurrence.plusWeeks(1);
         }
         return nextOccurrence;
     }
     */
}
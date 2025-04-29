package com.healthmanagement.service.course;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl; // 如果需要手動構建 Page
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

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
import java.util.HashMap;
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
                .orElseThrow(() -> new EntityNotFoundException("查無此會員編號"));
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
                .orElseThrow(() -> new EntityNotFoundException("查無此課程編號"));

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
                .orElseThrow(() -> new EntityNotFoundException("查無此會員編號"));
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

		
    @Override
    @Transactional(readOnly = true) // 標記為只讀事務
    public Page<CourseInfoDTO> getAllCoursesWithUserStatus(Integer userId, Integer page, Integer size, Boolean offersTrialOption, Integer dayOfWeek, String fullnessStatus) { // 新增 fullnessStatus 參數
        logger.info("查詢課程列表並包含使用者 {} 的狀態，頁碼: {}, 每頁: {}, 體驗課過濾: {}, 星期幾過濾: {}, 滿額狀態過濾: {}...", // 更新日誌
                    userId != null ? userId : "匿名", page, size, offersTrialOption != null ? offersTrialOption : "無過濾", dayOfWeek != null ? dayOfWeek : "無過濾", fullnessStatus != null ? fullnessStatus : "無過濾");

        // 1. 創建分頁請求
        Pageable pageable = PageRequest.of(page, size);

        // 2. 將 String fullnessStatus 轉換為 Boolean isFullForDao
        Boolean isFullForDao = null;
        if ("full".equals(fullnessStatus)) {
            isFullForDao = Boolean.TRUE;
             logger.debug("滿額狀態過濾轉換為 Boolean: TRUE");
        } else if ("notFull".equals(fullnessStatus)) {
            isFullForDao = Boolean.FALSE;
             logger.debug("滿額狀態過濾轉換為 Boolean: FALSE");
        } else {
             logger.debug("無滿額狀態過濾，Boolean 參數為 NULL");
        }


        // 3. 呼叫 CourseDAO 中新添加的 findCoursesWithFilters 方法，將所有篩選條件和分頁傳入
        // 這個方法能夠同時處理 offersTrialOption, dayOfWeek, isFull 和 Pageable
        Page<Course> coursePage = courseDAO.findCoursesWithFilters(
            pageable,
            offersTrialOption,  // 體驗課過濾參數
            dayOfWeek,          // 星期幾過濾參數
            isFullForDao        // 滿額狀態過濾參數 (Boolean 類型)
        );


        // 4. 獲取符合篩選條件的課程列表 (當前頁的數據) 和總數
        List<Course> courses = coursePage.getContent();
        long totalElements = coursePage.getTotalElements(); // <-- 這裡獲取的是經過後端篩選後的總數

        if (courses.isEmpty()) {
            logger.info("在當前篩選和分頁條件下，沒有找到任何課程。");
            return new PageImpl<>(Collections.emptyList(), pageable, totalElements); // 返回空分頁結果
        }
        logger.debug("找到 {} 條課程記錄 (當前頁)，總數 {}。", courses.size(), totalElements);


        // ... 後續的獲取使用者報名/預約狀態、組裝 CourseInfoDTO 的邏輯保持不變 ...
        // 這部分邏輯是基於 coursePage.getContent() 返回的當前頁數據進行的，所以不需要修改。

        final User finalUser = (userId != null) ? userDAO.findById(userId).orElse(null) : null;
        if (userId != null && finalUser == null) {
            logger.warn("傳入的使用者 ID {} 未找到對應的 User 實體，將無法查詢個人狀態。", userId);
        }

        // 4. 獲取常規報名相關數據 (已報名和候補人數，以及當前使用者的常規報名狀態)
        // 為了效率，我們只查詢當前頁課程的報名記錄
        List<Integer> currentCourseIds = courses.stream().map(Course::getId).collect(Collectors.toList());

        List<Enrollment> currentCoursesRegisteredEnrollments = enrollmentDAO.findByCourseIdInAndStatus(currentCourseIds, REGISTERED_STATUS);
        List<Enrollment> currentCoursesWaitingEnrollments = enrollmentDAO.findByCourseIdInAndStatus(currentCourseIds, WAITING_STATUS);

        final Map<Integer, Long> registeredCountsMap = currentCoursesRegisteredEnrollments.stream()
                .filter(e -> e.getCourse() != null)
                .collect(Collectors.groupingBy(e -> e.getCourse().getId(), Collectors.counting()));

        final Map<Integer, Long> waitlistCountsMap = currentCoursesWaitingEnrollments.stream()
                .filter(e -> e.getCourse() != null)
                .collect(Collectors.groupingBy(e -> e.getCourse().getId(), Collectors.counting()));

        final Map<Integer, Enrollment> userActiveEnrollmentsMap = (finalUser != null) ?
                // *** 呼叫 EnrollmentDAO 中根據使用者、課程 ID 列表和狀態列表查詢的方法 ***
                // 這個方法需要在 EnrollmentDAO 介面中定義
                enrollmentDAO.findByUserAndCourseIdInAndStatusNotIn(finalUser, currentCourseIds, INACTIVE_ENROLLMENT_STATUSES).stream()
                        .filter(e -> e.getCourse() != null)
                        .collect(Collectors.toMap(e -> e.getCourse().getId(), Function.identity())) :
                Collections.emptyMap();

        logger.debug("獲取了當前頁課程的常規報名計數和使用者狀態。");


        // 5. 獲取體驗預約相關數據 (下一個排程的已預約體驗人數，以及當前使用者的體驗預約狀態)
        // 過濾出當前頁提供體驗選項的課程
        List<Course> currentCoursesOfferingTrial = courses.stream()
                .filter(course -> course.getOffersTrialOption() != null && course.getOffersTrialOption())
                .collect(Collectors.toList());

        // 計算這些課程的下一個排程日期和時間
        Map<Integer, LocalDateTime> nextOccurrenceTimes = currentCoursesOfferingTrial.stream()
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
        Map<Integer, Integer> bookedTrialCountsMap = new java.util.HashMap<>();
        if (!courseIdsForTrialCountQuery.isEmpty()) {
             for (Integer courseId : courseIdsForTrialCountQuery) {
                 LocalDateTime nextTime = nextOccurrenceTimes.get(courseId);
                 if (nextTime != null) {
                     int count = trialBookingDAO.countTrialBookingsByCourseDateActualStartTimeAndStatusNotInNative(
                         courseId,
                         nextTime.toLocalDate(),
                         nextTime.toLocalTime(),
                         INACTIVE_TRIAL_STATUSES
                     );
                     bookedTrialCountsMap.put(courseId, count);
                 }
             }
             logger.debug("獲取了當前頁提供體驗選項課程的下一個排程體驗預約計數。");
        } else {
            logger.debug("當前頁沒有提供體驗選項的課程，跳過體驗預約計數查詢。");
        }

     // 如果有登入使用者，獲取其對這些提供體驗選項課程的活躍體驗預約記錄
     final Map<Integer, TrialBooking> userActiveTrialBookingsMap;

     if (finalUser != null && !currentCoursesOfferingTrial.isEmpty()) {
         List<Integer> courseIdsForUserTrialQuery = currentCoursesOfferingTrial.stream()
                                                       .map(Course::getId)
                                                       .collect(Collectors.toList());
         if (!courseIdsForUserTrialQuery.isEmpty()) {
             // *** 呼叫 TrialBookingDAO 中根據使用者、課程 ID 列表和狀態列表查詢的方法 ***
             // 這個方法需要在 TrialBookingDAO 介面中定義
             List<TrialBooking> userBookingsForCourses = trialBookingDAO.findByUserAndCourseIdInAndBookingStatusNotInAndBookingDateGreaterThanEqual(
                 finalUser,
                 courseIdsForUserTrialQuery,
                 INACTIVE_TRIAL_STATUSES,
                 LocalDate.now()
             );
             userActiveTrialBookingsMap = userBookingsForCourses.stream()
                  .filter(tb -> tb.getCourse() != null)
                  .collect(Collectors.toMap(tb -> tb.getCourse().getId(), Function.identity()));
             logger.debug("找到使用者 {} 對當前頁 {} 個課程的活躍體驗預約記錄。", userId, userActiveTrialBookingsMap.size());
         } else {
             userActiveTrialBookingsMap = Collections.emptyMap();
             logger.debug("使用者 {} 對當前頁沒有提供體驗選項的課程，跳過體驗預約記錄查詢。", userId);
         }
     } else {
          userActiveTrialBookingsMap = Collections.emptyMap();
          logger.debug("使用者未登入或當前頁沒有提供體驗選項的課程，跳過體驗預約記錄查詢。");
     }


        // 6. 將所有數據組裝到 CourseInfoDTO 列表中
     // 注意：isFull 和 isTrialFull 的計算仍然在這裡完成，即使數據已經被後端根據滿額狀態過濾過
     List<CourseInfoDTO> courseInfoDTOs = courses.stream().map(course -> {
         // 常規報名數據
         long registeredCount = registeredCountsMap.getOrDefault(course.getId(), 0L);
         long waitlistCount = waitlistCountsMap.getOrDefault(course.getId(), 0L);

         // 計算 isFull (這仍然需要，以便前端正確顯示狀態標籤)
         boolean isCourseActuallyFull = false;
         Integer maxCapacity = course.getMaxCapacity(); // 使用局部變數方便日誌
         if (maxCapacity != null && maxCapacity > 0) {
              isCourseActuallyFull = registeredCount >= maxCapacity;
         }
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
            // 直接使用 course.getOffersTrialOption() 的值，避免重新宣告同名變數
            Boolean courseOffersTrialOption = course.getOffersTrialOption() != null ? course.getOffersTrialOption() : false;
            Integer maxTrialCapacity = course.getMaxTrialCapacity();
            Integer bookedTrialCount = bookedTrialCountsMap.getOrDefault(course.getId(), 0);

            // 計算 isTrialFull (這仍然需要，以便前端正確顯示狀態標籤)
            boolean isCourseTrialActuallyFull = false;
            if (courseOffersTrialOption && maxTrialCapacity != null && maxTrialCapacity > 0) {
                 isCourseTrialActuallyFull = bookedTrialCount >= maxTrialCapacity;
            }
            // 使用者體驗預約狀態和 ID
            String userTrialBookingStatus = "未預約"; // 預設狀態
            Integer userTrialBookingId = null;
            // 使用 courseOffersTrialOption 劉進行判斷
            if (finalUser != null && courseOffersTrialOption) {
                 TrialBooking currentUserTrialBooking = userActiveTrialBookingsMap.get(course.getId());
                 if (currentUserTrialBooking != null) {
                      userTrialBookingStatus = currentUserTrialBooking.getBookingStatus();
                      userTrialBookingId = currentUserTrialBooking.getId();
                 }
            }

            User coach = course.getCoach();
            Integer coachId = (coach != null) ? coach.getId() : null;
            String coachName = (coach != null) ? coach.getName() : "N/A";

            return CourseInfoDTO.builder()
                    .id(course.getId())
                    .name(course.getName())
                    .description(course.getDescription())
                    // 使用 courseOffersTrialOption
                    .offersTrialOption(courseOffersTrialOption)
                    .maxTrialCapacity(maxTrialCapacity)
                    .bookedTrialCount(bookedTrialCount)
                    .userTrialBookingStatus(userTrialBookingStatus)
                    .userTrialBookingId(userTrialBookingId)
                    .coachId(coachId) // 新增 coachId
                    .coachName(coachName) // 新增 coachName
                    .dayOfWeek(course.getDayOfWeek()) // 新增 dayOfWeek
                    .startTime(course.getStartTime()) // 新增 startTime
                    .duration(course.getDuration()) // 新增 duration
                    .maxCapacity(course.getMaxCapacity()) // 新增 maxCapacity
                    .registeredCount((int) registeredCount) // 新增 registeredCount
                    .waitlistCount((int) waitlistCount) // 新增 waitlistCount
                    .userStatus(userStatus) // 新增 userStatus
                    .userEnrollmentId(userEnrollmentId) // 新增 userEnrollmentId
                    .isFull(isCourseActuallyFull) // 保留 isFull
                    .isTrialFull(isCourseTrialActuallyFull) // 保留 isTrialFull
                    .build();
        }).collect(Collectors.toList());

        // 7. 返回 Page<CourseInfoDTO>
        return new PageImpl<>(courseInfoDTOs, pageable, totalElements);
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
    
    @Override // 確保這個方法正確實現了 EnrollmentService 介面中的方法
    @Transactional(readOnly = true) // 查詢操作使用只讀事務
    public Map<Integer, Integer> getNextOccurrenceBookedTrialCounts(List<Course> courses) {
        logger.info("正在為 {} 個課程獲取下一個排程的體驗預約計數...", courses.size());

        Map<Integer, Integer> countsMap = new HashMap<>();
        // LocalDateTime now = LocalDateTime.now(); // 如果計算下一個排程時間需要當前時間，可以這樣獲取


        if (courses == null || courses.isEmpty()) {
            logger.warn("傳入的課程列表為空或 null，返回空 Map。");
            return countsMap; // 返回空的 Map
        }

        for (Course course : courses) {
            // 只為提供體驗選項且最大體驗容量大於 0 的課程計算計數
            if (course.getOffersTrialOption() != null && course.getOffersTrialOption() &&
                course.getMaxTrialCapacity() != null && course.getMaxTrialCapacity() > 0) {

                try {
                    // 1. 計算當前課程的下一個排程時間
                    // *** 請呼叫您現有的輔助方法 calculateNextCourseOccurrenceTime(course) ***
                    // *** 這個方法需要根據 course 的 dayOfWeek 和 startTime 計算下一個未來時間 ***
                    LocalDateTime nextOccurrenceTime = calculateNextCourseOccurrenceTime(course); // <-- 假設這個方法存在且正確實現


                    // *** 添加日誌：打印計算出的下一個排程時間 (除錯用) ***
                    logger.info("課程 ID {} ({} {}) 的下一個排程時間計算為: {}", course.getId(), course.getDayOfWeek(), course.getStartTime(), nextOccurrenceTime);


                    if (nextOccurrenceTime != null) {
                        // 2. 查詢該課程在該下一個排程時間的活躍體驗預約人數
                        // *** 請呼叫您的 TrialBookingDAO 中相應的查詢方法 ***
                        // *** 例如 countTrialBookingsByCourseDateActualStartTimeAndStatusNotInNative 或類似方法 ***
                        int bookedTrialCount = trialBookingDAO.countTrialBookingsByCourseDateActualStartTimeAndStatusNotInNative(
                            course.getId(),
                            nextOccurrenceTime.toLocalDate(), // 傳遞日期部分
                            nextOccurrenceTime.toLocalTime(), // 傳遞時間部分
                            INACTIVE_TRIAL_STATUSES // 傳遞非活躍狀態列表 (請確保 INACTIVE_TRIAL_STATUSES 已定義)
                        );

                        // *** 添加日誌：打印從 DAO 查詢到的體驗預約人數 (除錯用) ***
                        logger.info("課程 ID {} 在 {} 體驗預約計數查詢結果: {}", course.getId(), nextOccurrenceTime, bookedTrialCount);

                        // 3. 將計數存入 Map
                        countsMap.put(course.getId(), bookedTrialCount);

                    } else {
                        // 如果無法計算下一個排程時間 (例如課程時間設置有問題)，將計數設為 0
                        logger.warn("無法計算課程 ID {} 的下一個排程時間，將其體驗預約計數設為 0。", course.getId());
                        countsMap.put(course.getId(), 0); // 確保 Map 中有該課程的鍵，避免前端獲取時報錯
                    }
                } catch (Exception e) {
                    // 處理計算或 DAO 呼叫過程中可能發生的異常
                    logger.error("處理課程 ID {} 的體驗預約計數時發生錯誤，將其計數設為 0。", course.getId(), e);
                    countsMap.put(course.getId(), 0); // 確保異常情況下也有計數
                }

            } else {
                // 如果課程不提供體驗選項或最大體驗容量為 0，計數就是 0
                countsMap.put(course.getId(), 0);
                // logger.debug("課程 ID {} 不提供體驗或最大體驗容量為 0，體驗預約計數設為 0。", course.getId()); // 可選的調試日誌
            }
        }

        logger.info("完成獲取體驗預約計數，返回 Map 大小: {}", countsMap.size());
        return countsMap; // 返回包含所有課程體驗預約計數的 Map
    }
    
    // **MODIFICATION (Service Implementation): 實現支援分頁的查詢方法**
    @Override // 實現 EnrollmentService 介面中的方法
    public Page<EnrollmentDTO> findEnrollmentsPaginated(int page, int pageSize, String status) { // <-- 接收 status 參數
        // Spring Data JPA 的頁碼從 0 開始 (0 是第一頁)
        // 我們 Controller 傳來的 page 參數已經根據前端做了調整 (如果前端從 1 開始的話)
        int backendPage = page;
         if (backendPage < 0) {
            backendPage = 0;
        }

        logger.info("Service 查詢報名紀錄分頁與篩選 - 頁碼 (後端): {}, 每頁筆數: {}, 篩選狀態: {}", backendPage, pageSize, status);

        // 創建一個 Pageable 物件，用於傳給 Repository
        Pageable pageable = PageRequest.of(backendPage, pageSize);

        // **根據 status 是否為空來決定呼叫哪個 DAO 方法或使用哪個查詢**
        Page<Enrollment> enrollmentPage;
        if (status != null && !status.trim().isEmpty()) {
            // 如果 status 不為空，呼叫 DAO 中支援按狀態和分頁查詢的方法
            // TODO: 你需要在 EnrollmentDAO 中新增一個方法來支援這個查詢
            enrollmentPage = enrollmentDAO.findByStatus(status, pageable); // <-- 呼叫新的 DAO 方法
            logger.info("使用狀態 {} 進行篩選和分頁查詢。", status);
        } else {
            // 如果 status 為空，表示不篩選，只進行分頁查詢
            enrollmentPage = enrollmentDAO.findAll(pageable); // 呼叫 JpaRepository 內建的分頁查詢
             logger.info("不使用狀態篩選，只進行分頁查詢。");
        }


        logger.info("DAO 查詢完成，總筆數 (篩選後): {}", enrollmentPage.getTotalElements());

        // 將查詢到的 Page<Enrollment> 映射為 Page<EnrollmentDTO>
        Page<EnrollmentDTO> enrollmentDTOPage = enrollmentPage.map(enrollment -> {
            EnrollmentDTO dto = new EnrollmentDTO();
            dto.setId(enrollment.getId());
            // TODO: 在實際應用中，請確保 enrollment.getCourse() 和 enrollment.getUser() 不為 null，
            // 否則這裡可能會拋出 NullPointerException。
            // 可以使用 JOIN FETCH 在 Repository 查詢時一起載入，或在這裡增加 null 檢查。
             if (enrollment.getCourse() != null) {
                 dto.setCourseId(enrollment.getCourse().getId());
                 dto.setCourseName(enrollment.getCourse().getName());
             } else {
                 dto.setCourseId(null);
                 dto.setCourseName("未知課程");
             }
             if (enrollment.getUser() != null) {
                 dto.setUserId(enrollment.getUser().getId());
                 dto.setUserName(enrollment.getUser().getName());
             } else {
                 dto.setUserId(null);
                 dto.setUserName("未知使用者");
             }
            dto.setStatus(enrollment.getStatus());
            dto.setEnrollmentTime(enrollment.getEnrollmentTime());
            
            logger.info("Created EnrollmentDTO: {}", dto); // Log the fully populated DTO
            return dto;
        });
        return enrollmentDTOPage; // 回傳包含 DTO 和分頁資訊的 Page 物件
    }
    
	// 實現根據 ID 查找單個報名記錄的方法
    @Override // 確保這是對介面方法的實現
    public Optional<EnrollmentDTO> findEnrollmentById(Integer id) { // <-- 實現方法
        logger.info("Service: 根據 ID 查找報名記錄，ID: {}", id);
        // 使用 JpaRepository 提供的 findById 方法來查詢
        Optional<Enrollment> enrollmentOptional = enrollmentDAO.findById(id);

        // 將 Optional<Enrollment> 映射為 Optional<EnrollmentDTO>
        return enrollmentOptional.map(enrollment -> {
            EnrollmentDTO dto = new EnrollmentDTO();
            dto.setId(enrollment.getId());
            // TODO: 在實際應用中，請確保 enrollment.getCourse() 和 enrollment.getUser() 不為 null，
            // 否則這裡可能會拋出 NullPointerException。
            // 可以在這裡增加 null 檢查，或者在 DAO 層使用 JOIN FETCH
             if (enrollment.getCourse() != null) {
                 dto.setCourseId(enrollment.getCourse().getId());
                 dto.setCourseName(enrollment.getCourse().getName());
             } else {
                 dto.setCourseId(null);
                 dto.setCourseName("未知課程");
             }
             if (enrollment.getUser() != null) {
                 dto.setUserId(enrollment.getUser().getId());
                 dto.setUserName(enrollment.getUser().getName());
             } else {
                 dto.setUserId(null);
                 dto.setUserName("未知使用者");
             }
            dto.setStatus(enrollment.getStatus());
            dto.setEnrollmentTime(enrollment.getEnrollmentTime());
            
            logger.info("Created EnrollmentDTO: {}", dto);
            // TODO: 根據你的 Enrollment 實體和 DTO 定義，填充其他字段
            // 例如：
            // dto.setEnrollmentTime(enrollment.getEnrollmentTime());
            // dto.setCheckinTime(enrollment.getCheckinTime());
            // ... 等等 ...

            return dto; // 返回轉換後的 DTO
        });
    }
    
	// 實現依會員名稱查詢報名記錄的方法
    @Override // 確保這是對介面方法的實現
    public List<EnrollmentDTO> searchEnrollmentsByUserName(String userName) { // <-- 實現方法
        logger.info("Service: 依會員名稱查找報名記錄，名稱: {}", userName);

        // 檢查輸入名稱是否有效
        if (!StringUtils.hasText(userName)) {
             logger.warn("Service: 查詢會員名稱為空或只包含空格，返回空列表。");
             return List.of(); // 返回空列表，而不是拋異常
        }

        // 1. 呼叫 DAO 層方法根據會員名稱查詢報名記錄
        // 我們假設你的 Enrollment 實體有關聯到 User 實體 (會員)，
        // 並且 User 實體有 'name' 字段。
        // DAO 層方法將根據 User 的 name 字段進行查詢。
        // 使用 containsIgnoreCase 進行不區分大小寫的模糊查詢
        List<Enrollment> enrollments = enrollmentDAO.findByUser_NameContainingIgnoreCase(userName.trim()); // <-- 假設你有這個 DAO 方法

        logger.info("Service: 依會員名稱 '{}' 查詢，找到 {} 條報名記錄。", userName, enrollments.size());

        // 2. 將 Enrollment 實體列表轉換為 EnrollmentDTO 列表
        // 這裡使用你現有的轉換器進行轉換
        return enrollments.stream()
                .map(convertToDTOConverter::convertToEnrollmentDTO)
                .collect(Collectors.toList()); // 收集為列表
    }
}
package com.healthmanagement.service.course;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import com.healthmanagement.dao.course.CourseDAO;
import com.healthmanagement.dao.course.EnrollmentDAO;
import com.healthmanagement.dao.course.TrialBookingDAO;
import com.healthmanagement.dao.member.UserDAO;
import com.healthmanagement.dto.course.EnrollmentDTO;
import com.healthmanagement.dto.course.EnrollmentStatusUpdateDTO;
import com.healthmanagement.dto.course.CourseInfoDTO;
import com.healthmanagement.dto.course.CourseImageDTO;
import com.healthmanagement.model.course.Course;
import com.healthmanagement.model.course.Enrollment;
import com.healthmanagement.model.course.TrialBooking;
import com.healthmanagement.model.member.User;
import com.healthmanagement.model.course.CourseImage;

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
import java.util.Collection;
import java.util.Comparator;


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
    private TrialBookingDAO trialBookingDAO;

    private static final String REGISTERED_STATUS = "已報名";
    private static final String CANCELLED_STATUS = "已取消";
    private static final String WAITING_STATUS = "候補中";
    private static final String COMPLETED_STATUS = "已完成";
    private static final String NO_SHOW_STATUS = "未到場";
    private static final List<String> ACTIVE_ENROLLMENT_STATUSES = List.of(REGISTERED_STATUS, WAITING_STATUS);
    private static final List<String> INACTIVE_ENROLLMENT_STATUSES = List.of(CANCELLED_STATUS, COMPLETED_STATUS, NO_SHOW_STATUS);
    private static final List<String> INACTIVE_TRIAL_STATUSES = List.of(CANCELLED_STATUS, COMPLETED_STATUS, NO_SHOW_STATUS);

    private static final long ENROLLMENT_CANCEL_CUTOFF_HOURS = 24;

    // 計算課程的下一次發生時間，相對於當前日期時間 - 這是唯一一份實現
    private LocalDateTime calculateNextCourseOccurrenceTime(Course course) {
         if (course == null || course.getDayOfWeek() == null || course.getStartTime() == null) {
              logger.warn("Course ID {} has incomplete scheduling info (dayOfWeek: {}, startTime: {}). Cannot calculate next occurrence.",
                          course != null ? course.getId() : "N/A",
                          course != null ? course.getDayOfWeek() : "N/A", course != null ? course.getStartTime() : "N/A");
              return null;
         }
         LocalDate today = LocalDate.now();
         LocalTime nowTime = LocalTime.now();
         int dbDayOfWeek = course.getDayOfWeek();
         DayOfWeek courseDayOfWeek = DayOfWeek.of((dbDayOfWeek + 1) % 7 == 0 ? 7 : (dbDayOfWeek + 1) % 7);
         DayOfWeek todayDayOfWeek = today.getDayOfWeek();

         LocalDate nextDate = today;
         int daysUntilNext = courseDayOfWeek.getValue() - todayDayOfWeek.getValue();

         if (daysUntilNext < 0) {
             daysUntilNext += 7;
         }
         if (daysUntilNext == 0 && course.getStartTime().isBefore(nowTime)) {
             daysUntilNext = 7;
         }
         nextDate = today.plusDays(daysUntilNext);
         return LocalDateTime.of(nextDate, course.getStartTime());
    }

    private boolean isWithinHours(Course course, long hours) {
        LocalDateTime nextCourseTime = calculateNextCourseOccurrenceTime(course);
        if (nextCourseTime == null) {
             logger.warn("Cannot calculate next occurrence time for Course ID {}. Assuming within cutoff hours.", course != null ? course.getId() : "N/A");
             return true;
        }
        LocalDateTime now = LocalDateTime.now();
        return nextCourseTime.isAfter(now) && ChronoUnit.HOURS.between(now, nextCourseTime) < hours;
    }

    @Transactional
    private Enrollment performEnrollmentLogic(User user, Course course) {
        logger.info("執行核心報名邏輯，使用者 ID: {}，課程 ID: {}", user.getId(), course.getId());
        if (enrollmentDAO.existsByUserAndCourseAndStatusNotIn(user, course, INACTIVE_ENROLLMENT_STATUSES)) {
             logger.warn("使用者 ID {} 已有課程 ID {} 的活躍常規報名記錄。", user.getId(), course.getId());
             if (enrollmentDAO.existsByUserAndCourseAndStatus(user, course, REGISTERED_STATUS)) {
                 throw new IllegalStateException("您已報名此課程");
             } else if (enrollmentDAO.existsByUserAndCourseAndStatus(user, course, WAITING_STATUS)) {
                 throw new IllegalStateException("您已在候補名單中");
             } else {
                 throw new IllegalStateException("您已存在其他活躍狀態的報名記錄於此課程。");
             }
        }
        logger.info("使用者 ID {} 於課程 ID {} 不存在活躍常規報名記錄。", user.getId(), course.getId());

        if (isCourseFull(course.getId())) {
            logger.info("課程 ID {} 已滿，將使用者 ID {} 加入候補名單。", course.getId(), user.getId());
            Enrollment waitlistItem = Enrollment.builder()
                    .user(user)
                    .course(course)
                    .enrollmentTime(LocalDateTime.now())
                    .status(WAITING_STATUS)
                    .build();
            return enrollmentDAO.save(waitlistItem);
        } else {
            logger.info("課程 ID {} 未滿，將嘗試直接報名使用者 ID {}。", user.getId(), course.getId());
            Optional<Enrollment> existingCancelledEnrollmentOpt = enrollmentDAO.findByUserAndCourseAndStatus(user, course, CANCELLED_STATUS);
            if (existingCancelledEnrollmentOpt.isPresent()) {
                 logger.info("找到使用者 ID {} 於課程 ID {} 的已取消報名記錄 ID {}，將更新狀態。",
                             user.getId(), course.getId(), existingCancelledEnrollmentOpt.get().getId());
                 Enrollment existingCancelledEnrollment = existingCancelledEnrollmentOpt.get();
                 existingCancelledEnrollment.setEnrollmentTime(LocalDateTime.now());
                 existingCancelledEnrollment.setStatus(REGISTERED_STATUS);
                 return enrollmentDAO.save(existingCancelledEnrollment);
            } else {
                 logger.info("使用者 ID {} 於課程 ID {} 沒有已取消記錄，創建新的已報名記錄。", user.getId(), course.getId());
                 Enrollment newEnrollment = Enrollment.builder()
                         .user(user)
                         .course(course)
                         .enrollmentTime(LocalDateTime.now())
                         .status(REGISTERED_STATUS)
                         .build();
                 return enrollmentDAO.save(newEnrollment);
            }
        }
    }

    @Transactional
    @Override
    public EnrollmentDTO enrollUserToCourse(Integer userId, Integer courseId) {
        logger.info("使用者 ID {} 嘗試報名常規課程 ID: {}", userId, courseId);
        User user = userDAO.findById(userId)
                .orElseThrow(() -> new EntityNotFoundException("找不到使用者 ID: " + userId));
        Course course = courseDAO.findById(courseId)
                .orElseThrow(() -> new EntityNotFoundException("找不到課程 ID: " + courseId));

        if (isWithinHours(course, ENROLLMENT_CANCEL_CUTOFF_HOURS)) {
            LocalDateTime nextCourseTime = calculateNextCourseOccurrenceTime(course);
            logger.warn("使用者 ID {} 報名課程 ID {} 失敗：課程將於 {} 開始，距離不足 {} 小時。",
                       userId, courseId, nextCourseTime, ENROLLMENT_CANCEL_CUTOFF_HOURS);
            throw new IllegalStateException(String.format("報名失敗：課程將於 %s 開始，距離不足 %d 小時。",
                    nextCourseTime != null ? nextCourseTime.toString() : "未知時間", ENROLLMENT_CANCEL_CUTOFF_HOURS));
        }
        logger.info("確認課程 ID {} 報名時間符合提前預約時限 (提前 {} 小時)", courseId, ENROLLMENT_CANCEL_CUTOFF_HOURS);

        Enrollment resultEnrollment = performEnrollmentLogic(user, course);
        return convertToEnrollmentDTO(resultEnrollment);
    }

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

        if (isWithinHours(course, ENROLLMENT_CANCEL_CUTOFF_HOURS)) {
            LocalDateTime nextCourseTime = calculateNextCourseOccurrenceTime(course);
            logger.warn("取消報名 ID {} 失敗：課程將於 {} 開始，距離不足 {} 小時。",
                       enrollmentId, nextCourseTime, ENROLLMENT_CANCEL_CUTOFF_HOURS);
            throw new IllegalStateException(String.format("取消失敗：課程將於 %s 開始，距離不足 %d 小時。",
                    nextCourseTime != null ? nextCourseTime.toString() : "未知時間", ENROLLMENT_CANCEL_CUTOFF_HOURS));
        }
        logger.info("確認報名 ID {} 取消時間符合提前取消時限 (提前 {} 小時)", enrollmentId, ENROLLMENT_CANCEL_CUTOFF_HOURS);

        if (INACTIVE_ENROLLMENT_STATUSES.contains(enrollment.getStatus())) {
            logger.warn("報名 ID {} 狀態 {} 不允許取消。", enrollmentId, enrollment.getStatus());
            throw new IllegalStateException(String.format("報名狀態不正確，無法取消。目前狀態: %s", enrollment.getStatus()));
        }

        boolean wasRegistered = REGISTERED_STATUS.equals(enrollment.getStatus());
        logger.info("報名 ID {} 原狀態為 {}", enrollmentId, enrollment.getStatus());

        enrollment.setStatus(CANCELLED_STATUS);
        enrollmentDAO.save(enrollment);
        logger.info("報名 ID {} 狀態更新為 {}。", enrollmentId, CANCELLED_STATUS);

        if (wasRegistered) {
             if (!isCourseFull(course.getId())) {
                processWaitlist(course.getId());
             } else {
                 logger.info("課程 ID {} 仍滿，手動取消報名 ID {} 未觸發候補遞補。", course.getId(), enrollmentId);
             }
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<EnrollmentDTO> getEnrollmentsByUserId(Integer userId) {
        logger.info("查詢使用者 ID {} 的所有常規報名記錄...", userId);
        User user = userDAO.findById(userId)
                .orElseThrow(() -> new EntityNotFoundException("查無此會員編號"));
        List<Enrollment> userEnrollments = enrollmentDAO.findByUser(user);
        logger.info("找到使用者 ID {} 的 {} 条報名記錄。", userId, userEnrollments.size());

        return userEnrollments.stream()
                .map(this::convertToEnrollmentDTO)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<EnrollmentDTO> getEnrollmentsByCourseId(Integer courseId) {
        logger.info("查詢常規課程 ID {} 的所有報名記錄...", courseId);
        Course course = courseDAO.findById(courseId)
                .orElseThrow(() -> new EntityNotFoundException("查無此課程編號"));

        List<Enrollment> courseEnrollments = enrollmentDAO.findByCourse(course);
        logger.info("找到課程 ID {} 的 {} 條常規報名記錄。", courseId, courseEnrollments.size());
        return courseEnrollments.stream()
                .map(this::convertToEnrollmentDTO)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public boolean isCourseFull(Integer courseId) {
        logger.info("檢查常規課程 ID {} 是否已滿...", courseId);
        Course course = courseDAO.findById(courseId)
                .orElseThrow(() -> new EntityNotFoundException("Course not found with id: " + courseId));

        int enrolledCount = enrollmentDAO.countByCourseAndStatus(course, REGISTERED_STATUS);
        boolean isFull = course.getMaxCapacity() != null && course.getMaxCapacity() > 0 && enrolledCount >= course.getMaxCapacity();
        logger.info("常規課程 ID {} 已報名人數: {}，最大容量: {}，是否已滿: {}",
                   courseId, enrolledCount, course.getMaxCapacity(), isFull);
        return isFull;
    }

    @Override
    @Transactional(readOnly = true)
    public boolean isUserEnrolled(Integer userId, Integer courseId) {
        logger.info("檢查使用者 ID {} 是否已有效報名或候補常規課程 ID: {}", userId, courseId);
        User user = userDAO.findById(userId)
                .orElseThrow(() -> new EntityNotFoundException("查無此會員編號"));
        Course course = courseDAO.findById(courseId)
                .orElseThrow(() -> new EntityNotFoundException("Course not found with id: " + courseId));

        boolean isEnrolled = enrollmentDAO.existsByUserAndCourseAndStatusNotIn(user, course, INACTIVE_ENROLLMENT_STATUSES);
        logger.info("使用者 ID {} 是否已有效報名或候補常規課程 ID {}: {}", userId, courseId, isEnrolled);
        return isEnrolled;
    }

    @Override
    @Transactional(readOnly = true)
    public int getEnrolledCount(Integer courseId) {
        logger.info("查詢常規課程 ID {} 的已報名人數...", courseId);
        Course course = courseDAO.findById(courseId)
                .orElseThrow(() -> new EntityNotFoundException("Course not found with id: " + courseId));

        int count = enrollmentDAO.countByCourseAndStatus(course, REGISTERED_STATUS);
        logger.info("常規課程 ID {} 的已報名人數: {}", courseId, count);
        return count;
    }

    @Override
    @Transactional(readOnly = true)
    public boolean hasActiveEnrollmentsForCourse(Integer courseId) {
        logger.info("檢查課程 ID {} 是否存在活躍的常規報名記錄...", courseId);
        boolean hasActive = enrollmentDAO.existsByCourseIdAndStatusNotIn(courseId, INACTIVE_ENROLLMENT_STATUSES);
        logger.info("課程 ID {} 是否存在活躍的常規報名記錄: {}", courseId, hasActive);
        return hasActive;
    }

    // == 內部輔助方法，將 Course 實體及其相關數據轉換為 CourseInfoDTO ==
    private CourseInfoDTO convertToCourseInfoDTO(Course course, User finalUser,
                                                 Map<Integer, Long> registeredCountsMap,
                                                 Map<Integer, Long> waitlistCountsMap,
                                                 Map<Integer, Enrollment> userActiveEnrollmentsMap,
                                                 Map<Integer, Integer> bookedTrialCountsMap,
                                                 Map<Integer, TrialBooking> userActiveTrialBookingsMap) {

        // 常規報名數據
        long registeredCount = registeredCountsMap.getOrDefault(course.getId(), 0L);
        long waitlistCount = waitlistCountsMap.getOrDefault(course.getId(), 0L);

        // 計算 isFull
        boolean isCourseActuallyFull = false;
        Integer maxCapacity = course.getMaxCapacity();
        if (maxCapacity != null && maxCapacity > 0) {
            isCourseActuallyFull = registeredCount >= maxCapacity;
        }
        // 使用者常規報名狀態和 ID
        String userStatus = "未報名";
        Integer userEnrollmentId = null;
        if (finalUser != null) {
            Enrollment currentUserEnrollment = userActiveEnrollmentsMap.get(course.getId());
            if (currentUserEnrollment != null) {
                userStatus = currentUserEnrollment.getStatus();
                userEnrollmentId = currentUserEnrollment.getId();
            }
        }

        // 體驗預約數據 (只針對提供體驗選項的課程)
        Boolean courseOffersTrialOption = course.getOffersTrialOption() != null ?
                course.getOffersTrialOption() : false;
        Integer maxTrialCapacity = course.getMaxTrialCapacity();
        Integer bookedTrialCount = bookedTrialCountsMap.getOrDefault(course.getId(), 0);
        // 計算 isTrialFull
        boolean isCourseTrialActuallyFull = false;
        if (courseOffersTrialOption && maxTrialCapacity != null && maxTrialCapacity > 0) {
            isCourseTrialActuallyFull = bookedTrialCount >= maxTrialCapacity;
        }
        // 使用者體驗預約狀態和 ID
        String userTrialBookingStatus = "未預約";
        Integer userTrialBookingId = null;
        if (finalUser != null && courseOffersTrialOption) {
            TrialBooking currentUserTrialBooking = userActiveTrialBookingsMap.get(course.getId());
            if (currentUserTrialBooking != null) {
                userTrialBookingStatus = currentUserTrialBooking.getBookingStatus();
                userTrialBookingId = currentUserTrialBooking.getId();
            }
        }

        User coach = course.getCoach();
        Integer coachId = (coach != null) ? coach.getId() : null;
        String coachName = (coach != null) ? coach.getName() : "N/A"; // 這裡應該獲取教練的 Name 字段 (假設 User Entity 有 getName())

        // 將圖片列表添加到 CourseInfoDTO Builder 中
        // 假設 Course Entity 已經載入了 images 關聯
        List<CourseImageDTO> imageDtoList = course.getImages() != null ? // === 修正: 將 List<CourseImageDto> 改為 List<CourseImageDTO> ===
                course.getImages().stream()
                    // 根據 imageOrder 屬性進行排序
                    // 修正: 將 image.getOrder() 改為 image.getImageOrder()
                    .sorted(Comparator.comparing(image -> image.getImageOrder() != null ? image.getImageOrder() : Integer.MAX_VALUE))
                    // 映射為 CourseImageDTO 物件
                    // 修正: 確保使用 CourseImageDTO.builder()
                    .map(image -> CourseImageDTO.builder()
                        .imageUrl(image.getImageUrl())
                        // 修正: 將 .order() 改為 .imageOrder()，並使用 image.getImageOrder()
                        .imageOrder(image.getImageOrder())
                        .build())
                    .collect(Collectors.toList())
                : Collections.emptyList();

        return CourseInfoDTO.builder()
                .id(course.getId())
                .name(course.getName())
                .description(course.getDescription())
                .offersTrialOption(courseOffersTrialOption)
                .maxTrialCapacity(maxTrialCapacity)
                .bookedTrialCount(bookedTrialCount)
                .userTrialBookingStatus(userTrialBookingStatus)
                .userTrialBookingId(userTrialBookingId)
                .coachId(coachId)
                .coachName(coachName)
                .dayOfWeek(course.getDayOfWeek())
                .startTime(course.getStartTime())
                .duration(course.getDuration())
                .maxCapacity(maxCapacity)
                .registeredCount((int) registeredCount)
                .waitlistCount((int) waitlistCount)
                .userStatus(userStatus)
                .userEnrollmentId(userEnrollmentId)
                .isFull(isCourseActuallyFull) // 設置 isFull 欄位
                .isTrialFull(isCourseTrialActuallyFull) // 設置 isTrialFull 欄位
                .images(imageDtoList)
                .build();
    }
    // ===============================================================


    @Override
    @Transactional(readOnly = true)
    public Page<CourseInfoDTO> getAllCoursesWithUserStatus(Integer userId, Integer page, Integer size, Boolean offersTrialOption, Integer dayOfWeek, String fullnessStatus) {
        logger.info("查詢課程列表並包含使用者 {} 的狀態，頁碼: {}, 每頁: {}, 體驗課過濾: {}, 星期幾過濾: {}, 滿額狀態過濾: {}...",
                    userId != null ? userId : "匿名", page, size, offersTrialOption != null ? offersTrialOption : "無過濾",
                    dayOfWeek != null ? dayOfWeek : "無過濾", fullnessStatus != null ? fullnessStatus : "無過濾");

        Pageable pageable = PageRequest.of(page, size);

        // 將 String fullnessStatus 轉換為 Boolean filterIsFull
        // 使用明確的變數名稱以避免混淆
        final Boolean filterIsFull;
        if ("full".equals(fullnessStatus)) {
            filterIsFull = Boolean.TRUE;
            logger.debug("滿額狀態過濾轉換為 Boolean: TRUE");
        } else if ("notFull".equals(fullnessStatus)) {
            filterIsFull = Boolean.FALSE;
            logger.debug("滿額狀態過濾轉換為 Boolean: FALSE");
        } else {
             filterIsFull = null;
             logger.debug("無滿額狀態過濾，Boolean 參數為 NULL");
        }


        Page<Course> coursePage = courseDAO.findCoursesWithFilters(
            pageable,
            offersTrialOption,
            dayOfWeek
        );

        List<Course> courses = coursePage.getContent();
        long totalElements = coursePage.getTotalElements();

        if (courses.isEmpty()) {
            logger.info("在當前篩選和分頁條件下，沒有找到任何課程。");
            return new PageImpl<>(Collections.emptyList(), pageable, totalElements);
        }
        logger.debug("找到 {} 條課程記錄 (當前頁)，總數 {}。", courses.size(), totalElements);

        final User finalUser = (userId != null) ?
                userDAO.findById(userId).orElse(null) : null;
        if (userId != null && finalUser == null) {
            logger.warn("傳入的使用者 ID {} 未找到對應的 User 實體，將無法查詢個人狀態。", userId);
        }

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
                enrollmentDAO.findByUserAndCourseIdInAndStatusNotIn(finalUser, currentCourseIds, INACTIVE_ENROLLMENT_STATUSES).stream()
                        .filter(e -> e.getCourse() != null)
                        .collect(Collectors.toMap(e -> e.getCourse().getId(), Function.identity())) :
                Collections.emptyMap();
        logger.debug("獲取了當前頁課程的常規報名計數和使用者狀態。");

        List<Course> currentCoursesOfferingTrial = courses.stream()
                .filter(course -> course.getOffersTrialOption() != null && course.getOffersTrialOption())
                .collect(Collectors.toList());
        Map<Integer, LocalDateTime> nextOccurrenceTimes = currentCoursesOfferingTrial.stream()
                .collect(Collectors.toMap(
                        Course::getId,
                        this::calculateNextCourseOccurrenceTime
                ));
        List<Integer> courseIdsForTrialCountQuery = nextOccurrenceTimes.entrySet().stream()
                .filter(entry -> entry.getValue() != null)
                .map(Map.Entry::getKey)
                .collect(Collectors.toList());

        Map<Integer, Integer> bookedTrialCountsMap = new HashMap<>();
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
                     logger.debug("課程 ID {} 在 {} 體驗預約計數查詢結果: {}", courseId, nextTime, count);
                     bookedTrialCountsMap.put(courseId, count);
                 }
             }
             logger.debug("獲取了當前頁提供體驗選項課程的下一個排程體驗預約計數。");
        } else {
            logger.debug("當前頁沒有提供體驗選項的課程，跳過體驗預約計數查詢。");
        }

     final Map<Integer, TrialBooking> userActiveTrialBookingsMap;
     if (finalUser != null && !currentCoursesOfferingTrial.isEmpty()) {
         List<Integer> courseIdsForUserTrialQuery = currentCoursesOfferingTrial.stream()
                                                       .map(Course::getId)
                                                        .collect(Collectors.toList());
         if (!courseIdsForUserTrialQuery.isEmpty()) {
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

     List<CourseInfoDTO> courseInfoDTOs = courses.stream()
         .map(course -> convertToCourseInfoDTO(course, finalUser,
                                               registeredCountsMap, waitlistCountsMap,
                                               userActiveEnrollmentsMap, bookedTrialCountsMap,
                                               userActiveTrialBookingsMap))
         .collect(Collectors.toList());

        if (filterIsFull != null) {
            logger.info("在 Service 層根據滿額狀態 {} 過濾課程列表。", fullnessStatus);
            courseInfoDTOs = courseInfoDTOs.stream()
                // 這裡使用 filterIsFull 變數，它是 final 的
                // 並且呼叫 DTO 的 isFull() 方法
                .filter(dto -> filterIsFull.equals(dto.isFull())) // <--- 這裡使用了 filterIsFull.equals(dto.isFull())
                .collect(Collectors.toList());
            logger.info("過濾後剩下 {} 條課程記錄。", courseInfoDTOs.size());
        }

        return new PageImpl<>(courseInfoDTOs, pageable, totalElements);
    }

    @Transactional
    @Override
    public EnrollmentDTO addWaitlistItem(Integer userId, Integer courseId) {
        logger.info("嘗試將使用者 ID {} 加入課程 ID {} 的候補名單...", userId, courseId);
        User user = userDAO.findById(userId)
                .orElseThrow(() -> new EntityNotFoundException("User not found with id: " + userId));
        Course course = courseDAO.findById(courseId)
                .orElseThrow(() -> new EntityNotFoundException("Course not found with id: " + courseId));

        if (enrollmentDAO.existsByUserAndCourseAndStatusNotIn(user, course, INACTIVE_ENROLLMENT_STATUSES)) {
             logger.warn("使用者 ID {} 已有課程 ID {} 的活躍常規報名記錄。", user.getId(), course.getId());
             if (enrollmentDAO.existsByUserAndCourseAndStatus(user, course, REGISTERED_STATUS)) {
                 throw new IllegalStateException("您已報名此課程");
             } else if (enrollmentDAO.existsByUserAndCourseAndStatus(user, course, WAITING_STATUS)) {
                 throw new IllegalStateException("您已在候補名單中");
             } else {
                 throw new IllegalStateException("您已存在其他活躍狀態的報名記錄於此課程。");
             }
        }
        logger.info("使用者 ID {} 於課程 ID {} 不存在活躍常規報名記錄。", user.getId(), course.getId());

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
        return convertToEnrollmentDTO(savedWaitlistItem);
    }

    @Transactional
    @Override
    public void processWaitlist(Integer courseId) {
        logger.info("處理常規課程 ID {} 的候補名單自動遞補...", courseId);
        Course course = courseDAO.findById(courseId)
                .orElseThrow(() -> new EntityNotFoundException("Course not found with id: " + courseId));

        logger.info("課程 ID {} 接受常規報名，進行候補遞補處理。", courseId);

        long registeredCount = enrollmentDAO.countByCourseAndStatus(course, REGISTERED_STATUS);
        Integer maxCapacity = course.getMaxCapacity();

        if (maxCapacity != null && maxCapacity > 0 && registeredCount < maxCapacity) {
            logger.info("課程 ID {} 有空位，已報名人數: {}，最大容量: {}", course.getId(), registeredCount, maxCapacity);
            List<Enrollment> waitingList = enrollmentDAO.findByCourseAndStatusOrderByEnrollmentTimeAsc(course, WAITING_STATUS);
            if (waitingList.isEmpty()) {
                logger.info("課程 ID {} 候補名單為空，無需遞補。", course.getId());
                return;
            }
            logger.info("課程 ID {} 找到 {} 位候補中的使用者。", course.getId(), waitingList.size());

            int slotsAvailable = (int) Math.max(0, maxCapacity - registeredCount);

            int promotedCount = 0;
            for (int i = 0; i < Math.min(slotsAvailable, waitingList.size()); i++) {
                Enrollment enrollment = waitingList.get(i);
                 if (WAITING_STATUS.equals(enrollment.getStatus())) {
                     enrollment.setStatus(REGISTERED_STATUS);
                     enrollmentDAO.save(enrollment);
                     promotedCount++;
                     logger.info("候補名單自動遞補：使用者 {} (報名 ID {}) 已從候補遞補為已報名 課程 ID: {}",
                             enrollment.getUser() != null ? enrollment.getUser().getId() : "Unknown User",
                             enrollment.getId(), course.getId());
                 } else {
                      logger.warn("候補報名 ID {} 狀態 {} 不正確，跳過遞補。", enrollment.getId(), enrollment.getStatus());
                 }
            }
            logger.info("課程 ID {} 候補名單自動遞補完成，遞補了 {} 位使用者。", course.getId(), promotedCount);
        } else {
            logger.info("課程 ID {} 已滿、最大容量未設定或無空位，無需遞補。 已報名人數: {}，最大容量: {}", course.getId(), registeredCount, maxCapacity);
        }
    }

    @Transactional
    @Override
    public EnrollmentDTO updateEnrollmentStatus(Integer enrollmentId, EnrollmentStatusUpdateDTO updateDTO) {
        String newStatus = updateDTO.getStatus();
        logger.info("嘗試手動更新報名 ID {} 的狀態為 {}", enrollmentId, newStatus);
        Enrollment enrollment = enrollmentDAO.findById(enrollmentId)
                .orElseThrow(() -> new EntityNotFoundException("找不到報名 ID: + enrollmentId"));
        String oldStatus = enrollment.getStatus();

        if (oldStatus.equals(newStatus)) {
             logger.warn("報名 ID {} 狀態已為 {}，無需更新。", enrollmentId, newStatus);
             return convertToEnrollmentDTO(enrollment);
        }
        if (INACTIVE_ENROLLMENT_STATUSES.contains(oldStatus)) {
             logger.warn("無法從目前狀態 '{}' 更改報名 ID {} 的狀態。", oldStatus, enrollmentId);
             throw new IllegalStateException(String.format("無法從目前狀態 '%s' 更改報名狀態。", oldStatus));
        }

        enrollment.setStatus(newStatus);
        Enrollment updatedEnrollmentEntity = enrollmentDAO.save(enrollment);
        logger.info("報名 ID {} 狀態成功從 {} 更新為 {}", enrollmentId, oldStatus, newStatus);

        boolean wasActive = ACTIVE_ENROLLMENT_STATUSES.contains(oldStatus);
        boolean isNowCancelled = CANCELLED_STATUS.equals(newStatus);
        if (wasActive && isNowCancelled) {
            logger.info("報名 ID {} 狀態從活躍 ({}) 變為已取消，觸發候補遞補流程。", enrollmentId, oldStatus);
            Course course = updatedEnrollmentEntity.getCourse();
            if (course != null) {
                 if (!isCourseFull(course.getId())) {
                    processWaitlist(course.getId());
                 } else {
                     logger.info("課程 ID {}仍滿，手動取消報名 ID {} 未觸發候補遞補。", course.getId(), enrollmentId);
                 }
            } else {
                logger.warn("報名 ID {} 關聯的課程為 null，無法觸發候補遞補流程。", enrollmentId);
            }
        } else {
            logger.info("報名 ID {} 狀態從 {} 更新為 {}，無需觸發候補遞補流程。", enrollmentId, oldStatus, newStatus);
        }
        return convertToEnrollmentDTO(updatedEnrollmentEntity);
    }

    // 計算課程最近一次過去的發生時間，相對於給定的時間點
    private LocalDateTime calculateLastPastCourseOccurrenceTime(Course course, LocalDateTime relativeTo) {
         if (course == null || course.getDayOfWeek() == null || course.getStartTime() == null) {
              logger.warn("Course ID {} has incomplete scheduling info (dayOfWeek: {}, startTime: {}). Cannot calculate last occurrence.",
                          course != null ? course.getId() : "N/A",
                          course != null ? course.getDayOfWeek() : "N/A", course != null ? course.getStartTime() : "N/A");
              return null;
         }
         LocalDate relativeDate = relativeTo.toLocalDate();
         LocalTime relativeTime = relativeTo.toLocalTime();
         int dbDayOfWeek = course.getDayOfWeek();
         if (dbDayOfWeek < 0 || dbDayOfWeek > 6) {
             logger.warn("Course ID {} has invalid dayOfWeek value: {}. Cannot calculate last occurrence.", course.getId(), dbDayOfWeek);
             return null;
         }
         DayOfWeek courseDayOfWeek = DayOfWeek.of((dbDayOfWeek + 1) % 7 == 0 ? 7 : (dbDayOfWeek + 1) % 7);
         DayOfWeek relativeDayOfWeek = relativeTo.getDayOfWeek();

         LocalDate lastDate = relativeDate.minusDays((relativeDayOfWeek.getValue() - courseDayOfWeek.getValue() + 7) % 7);
         if (LocalDateTime.of(lastDate, course.getStartTime()).isAfter(relativeTo)) {
              lastDate = lastDate.minusWeeks(1);
         }
         return LocalDateTime.of(lastDate, course.getStartTime());
    }


    @Scheduled(cron = "0 0 2 * * ?")
    @Transactional
    public void processPastDueEnrollments() {
        logger.info("Running scheduled task: Processing past due enrollments...");
        LocalDateTime now = LocalDateTime.now();
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
                logger.error("Enrollment ID {} has null course. Skipping.", enrollment != null ? enrollment.getId() : "N/A");
                continue;
            }

            LocalDateTime lastOccurrence = calculateLastPastCourseOccurrenceTime(course, now);
            if (lastOccurrence != null && now.isAfter(lastOccurrence)) {
                enrollment.setStatus(NO_SHOW_STATUS);
                enrollmentDAO.save(enrollment);
                updatedCount++;
                logger.info("Marked Enrollment ID {} for Course {} as {}. Last occurrence: {}",
                            enrollment.getId(), course.getName(), NO_SHOW_STATUS, lastOccurrence);
            }
        }
        logger.info("Finished processing past due enrollments. Updated: {}", updatedCount);
    }

    @Override
    @Transactional(readOnly = true)
    public Map<Integer, Integer> getNextOccurrenceBookedTrialCounts(List<Course> courses) {
        logger.info("這個 getNextOccurrenceBookedTrialCounts 方法的實際實作在 EnrollmentServiceImpl 中，為 getAllCoursesWithUserStatus 提供數據。");
        logger.info("正在為 {} 個課程獲取下一個排程的體驗預約計數...", courses != null ? courses.size() : 0);
        Map<Integer, Integer> countsMap = new HashMap<>();

        if (courses == null || courses.isEmpty()) {
            logger.warn("傳入的課程列表為空或 null，返回空 Map。");
            return countsMap;
        }

        for (Course course : courses) {
             if (course == null) {
                 logger.warn("課程列表包含 null 項目，跳過處理。");
                 continue;
             }
            if (course.getOffersTrialOption() != null && course.getOffersTrialOption() &&
                course.getMaxTrialCapacity() != null && course.getMaxTrialCapacity() > 0) {

                try {
                    LocalDateTime nextOccurrenceTime = calculateNextCourseOccurrenceTime(course);

                    logger.debug("課程 ID {} ({} {}) 的下一個排程時間計算為: {}", course.getId(), course.getDayOfWeek(), course.getStartTime(), nextOccurrenceTime);

                    if (nextOccurrenceTime != null) {
                        int bookedTrialCount = trialBookingDAO.countTrialBookingsByCourseDateActualStartTimeAndStatusNotInNative(
                            course.getId(),
                            nextOccurrenceTime.toLocalDate(),
                            nextOccurrenceTime.toLocalTime(),
                            INACTIVE_TRIAL_STATUSES
                        );
                        logger.debug("課程 ID {} 在 {} 體驗預約計數查詢結果: {}", course.getId(), nextOccurrenceTime, bookedTrialCount);
                        countsMap.put(course.getId(), bookedTrialCount);
                    } else {
                        logger.warn("無法計算課程 ID {} 的下一個排程時間，將其體驗預約計數設為 0。", course.getId());
                        countsMap.put(course.getId(), 0);
                    }
                } catch (Exception e) {
                    logger.error("處理課程 ID {} 的體驗預約計數時發生錯誤，將其計數設為 0。", course.getId(), e);
                    countsMap.put(course.getId(), 0);
                }

            } else {
                countsMap.put(course.getId(), 0);
            }
        }

        logger.info("完成獲取體驗預約計數，返回 Map 大小: {}", countsMap.size());
        return countsMap;
    }


    @Override
    @Transactional(readOnly = true)
    public Page<EnrollmentDTO> findEnrollmentsPaginated(int page, int pageSize, String status) {
        int backendPage = page;
        if (backendPage < 0) {
            backendPage = 0;
        }

        logger.info("Service 查詢報名紀錄分頁與篩選 - 頁碼 (後端): {}, 每頁筆數: {}, 篩選狀態: {}", backendPage, pageSize, status);
        Pageable pageable = PageRequest.of(backendPage, pageSize);

        Page<Enrollment> enrollmentPage;
        if (status != null && !status.trim().isEmpty()) {
            enrollmentPage = enrollmentDAO.findByStatus(status, pageable);
            logger.info("使用狀態 {} 進行篩選和分頁查詢。", status);
        } else {
            enrollmentPage = enrollmentDAO.findAll(pageable);
            logger.info("不使用狀態篩選，只進行分頁查詢。");
        }

        logger.info("DAO 查詢完成，總筆數 (篩選後): {}", enrollmentPage.getTotalElements());
        Page<EnrollmentDTO> enrollmentDTOPage = enrollmentPage.map(this::convertToEnrollmentDTO);
        return enrollmentDTOPage;
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<EnrollmentDTO> findEnrollmentById(Integer id) {
        logger.info("Service: 根據 ID 查找報名記錄，ID: {}", id);
        Optional<Enrollment> enrollmentOptional = enrollmentDAO.findById(id);
        return enrollmentOptional.map(this::convertToEnrollmentDTO);
    }

    @Override
    @Transactional(readOnly = true)
    public List<EnrollmentDTO> searchEnrollmentsByUserName(String userName) {
        logger.info("Service: 依會員名稱查找報名記錄，名稱: {}", userName);
        if (!StringUtils.hasText(userName)) {
             logger.warn("Service: 查詢會員名稱為空或只包含空格，返回空列表。");
             return Collections.emptyList();
        }

        List<Enrollment> enrollments = enrollmentDAO.findByUser_NameContainingIgnoreCase(userName.trim());

        logger.info("Service: 依會員名稱 '{}' 查詢，找到 {} 條報名記錄。", userName, enrollments.size());
        return enrollments.stream()
                .map(this::convertToEnrollmentDTO)
                .collect(Collectors.toList());
    }

    // == 實作 isUserTrialBooked(Integer userId, Integer courseId) 方法 == - 這是唯一一份實現
    @Override
    @Transactional(readOnly = true)
    public boolean isUserTrialBooked(Integer userId, Integer courseId) {
        boolean isBooked = trialBookingDAO.existsByUserIdAndCourseIdAndBookingStatusNotIn(
            userId,
            courseId,
            INACTIVE_TRIAL_STATUSES
        );
        return isBooked;
    }


 // 在 EnrollmentServiceImpl.java 中找到 convertToEnrollmentDTO 方法

 // == 內部輔助方法，將 Enrollment 實體轉換為 EnrollmentDTO ==
 private EnrollmentDTO convertToEnrollmentDTO(Enrollment enrollment) {
     if (enrollment == null) {
         return null;
     }
     EnrollmentDTO dto = new EnrollmentDTO();
     dto.setId(enrollment.getId());
     dto.setStatus(enrollment.getStatus());
     dto.setEnrollmentTime(enrollment.getEnrollmentTime());

     // ... 其他欄位 (userId, userName, courseId, courseName) 的設定 ...
     // 確保 courseId 和 courseName 也是從 enrollment.getCourse() 獲取

     if (enrollment.getCourse() != null) {
         dto.setCourseId(enrollment.getCourse().getId());
         dto.setCourseName(enrollment.getCourse().getName());

         // === 修正: 安全地獲取教練資訊 ===
         if (enrollment.getCourse().getCoach() != null) {
              // 假設 User Entity (教練) 有 getName() 方法
              dto.setCoachName(enrollment.getCourse().getCoach().getName());
         } else {
              dto.setCoachName("未知教練"); // 如果教練物件為 null，設定為未知教練
         }
         // ========================================
     } else {
         // 如果課程物件為 null，則課程和教練資訊都設定為未知
         dto.setCourseId(null);
         dto.setCourseName("未知課程");
         dto.setCoachName("未知教練");
     }

     // 你可能還有其他字段需要設置，例如 dayOfWeek, startTime
      if (enrollment.getCourse() != null) {
          dto.setDayOfWeek(enrollment.getCourse().getDayOfWeek());
          dto.setStartTime(enrollment.getCourse().getStartTime());
      } else {
          dto.setDayOfWeek(null);
          dto.setStartTime(null);
      }


     return dto;
 }
}
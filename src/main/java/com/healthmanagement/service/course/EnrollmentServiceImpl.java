package com.healthmanagement.service.course;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.healthmanagement.dao.course.CourseDAO;
import com.healthmanagement.dao.course.EnrollmentDAO;
import com.healthmanagement.dao.member.UserDAO;
import com.healthmanagement.dto.course.EnrollmentDTO;
import com.healthmanagement.dto.course.EnrollmentStatusUpdateDTO;
import com.healthmanagement.dto.course.CourseInfoDTO;
import com.healthmanagement.model.course.Course;
import com.healthmanagement.model.course.Enrollment;
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
import java.util.stream.Collectors;

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
    private ConvertToDTO convertToDTOConverter;

    private static final String REGISTERED_STATUS = "已報名";
    private static final String CANCELLED_STATUS = "已取消";
    private static final String WAITING_STATUS = "候補中";
    private static final String COMPLETED_STATUS = "已完成";
    private static final String NO_SHOW_STATUS = "未到場";

    // 活躍的預約狀態列表（用於檢查是否已活躍預約，排除已取消等狀態）
    private static final List<String> ACTIVE_ENROLLMENT_STATUSES = List.of(REGISTERED_STATUS, WAITING_STATUS);
    // 非活躍狀態列表（用於查詢時排除）
    private static final List<String> INACTIVE_ENROLLMENT_STATUSES = List.of(CANCELLED_STATUS, COMPLETED_STATUS, NO_SHOW_STATUS);

    // 設定報名和取消檢查的時限（例如提前 24 小時）
    private static final long ENROLLMENT_CANCEL_CUTOFF_HOURS = 24;

    // 計算課程的下一次發生時間，相對於當前日期時間
    private LocalDateTime calculateNextCourseOccurrenceTime(Course course) {
        if (course.getDayOfWeek() == null || course.getStartTime() == null) {
             logger.warn("Course ID {} has incomplete scheduling info (dayOfWeek: {}, startTime: {}). Cannot calculate next occurrence.",
                        course.getId(), course.getDayOfWeek(), course.getStartTime());
            return null;
        }
        LocalDate today = LocalDate.now();
        LocalTime nowTime = LocalTime.now();
        int dayOfWeekValue = course.getDayOfWeek();
        if (dayOfWeekValue < 0 || dayOfWeekValue > 6) {
             logger.warn("Course ID {} has invalid dayOfWeek value: {}. Cannot calculate next occurrence.", course.getId(), dayOfWeekValue); // **更動：使用 logger**
             return null;
        }
        DayOfWeek courseDayOfWeek = DayOfWeek.of(dayOfWeekValue % 7 + 1);

        LocalDate nextDate = today;
        int daysUntilNext = courseDayOfWeek.getValue() - today.getDayOfWeek().getValue();

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
             return true; // 如果無法計算時間，視為在時限內（阻止報名/取消）
        }
        LocalDateTime now = LocalDateTime.now();
        // 檢查是否在未來，並且距離小於指定小時數
        // 同时阻止預約過去的時間點 - 但 calculateNextCourseOccurrenceTime 已經確保是未來的時間
        return nextCourseTime.isAfter(now) && ChronoUnit.HOURS.between(now, nextCourseTime) < hours;
    }

	// 處理檢查是否已報名/候補、課程是否已滿、以及更新已取消記錄。
    @Transactional
    private Enrollment performEnrollmentLogic(User user, Course course) {
        logger.info("執行核心報名邏輯，使用者 ID: {}，課程 ID: {}", user.getId(), course.getId());
        // 檢查使用者是否已經有「已報名」或「候補中」的記錄
        Optional<Enrollment> existingActiveEnrollmentOpt = enrollmentDAO.findByUserAndCourseAndStatus(user, course, REGISTERED_STATUS)
                .or(() -> enrollmentDAO.findByUserAndCourseAndStatus(user, course, WAITING_STATUS)); // 查找已報名 或 候補中
        if (existingActiveEnrollmentOpt.isPresent()) {
            Enrollment existingActiveEnrollment = existingActiveEnrollmentOpt.get();
             logger.warn("使用者 ID {} 已有狀態為 {} 的報名記錄 ID {} 於課程 ID {}。",
                        user.getId(), existingActiveEnrollment.getStatus(), existingActiveEnrollment.getId(), course.getId());
            if (REGISTERED_STATUS.equals(existingActiveEnrollment.getStatus())) {
                 throw new IllegalStateException("您已報名此課程");
            } else if (WAITING_STATUS.equals(existingActiveEnrollment.getStatus())) {
                 throw new IllegalStateException("您已在候補名單中");
            }
        }
        // 檢查課程是否已滿 (優先判斷)
        if (isCourseFull(course.getId())) {
            logger.info("課程 ID {} 已滿，將使用者 ID {} 加入候補名單。", course.getId(), user.getId());
            // 課程已滿，直接加入候補名單 (不論之前是否有取消記錄，因為候補名單可以有多筆取消後再候補的記錄)
            Enrollment waitlistItem = Enrollment.builder()
                    .user(user)
                    .course(course)
                    .enrollmentTime(LocalDateTime.now())
                    .status(WAITING_STATUS) // 設定為候補中
                    .build();
            return enrollmentDAO.save(waitlistItem);
        } else {
            logger.info("課程 ID {} 未滿，將嘗試直接報名使用者 ID {}。", course.getId(), user.getId());
            // 課程未滿，檢查是否有之前的「已取消」記錄
            // 這裡可以選擇是否重用已取消的記錄，或者創建新的。重用可以保持歷史記錄的關聯性。
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

	 // 報名常規課程（包含檢查是否為體驗課程和 24 小時限制）
    @Transactional
    @Override
    public EnrollmentDTO enrollUserToCourse(Integer userId, Integer courseId) {
        logger.info("使用者 ID {} 嘗試報名常規課程 ID: {}", userId, courseId);
        User user = userDAO.findById(userId)
                .orElseThrow(() -> new EntityNotFoundException("找不到使用者 ID: " + userId));
        Course course = courseDAO.findById(courseId)
                .orElseThrow(() -> new EntityNotFoundException("找不到課程 ID: " + courseId));
        // 檢查課程是否在 24 小時內開始 (報名截止時間)
        // 這裡假設報名必須在課程開始前至少 24 小時
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
                .orElseThrow(() -> new EntityNotFoundException("找不到報名 ID: " + enrollmentId));
        Course course = enrollment.getCourse();
        if (course == null) {
             logger.error("報名 ID {} 關聯的課程為 null，無法檢查取消時限。", enrollmentId);
             throw new IllegalStateException("報名記錄關聯的課程無效，無法取消。");
        }
        // 檢查課程是否在 24 小時內開始 (取消截止時間)
        // 這裡假設取消也必須在課程開始前至少 24 小時
        if (isWithinHours(course, ENROLLMENT_CANCEL_CUTOFF_HOURS)) {
             LocalDateTime nextCourseTime = calculateNextCourseOccurrenceTime(course);
             logger.warn("取消報名 ID {} 失敗：課程將於 {} 開始，距離不足 {} 小時。",
                        enrollmentId, nextCourseTime, ENROLLMENT_CANCEL_CUTOFF_HOURS);
            throw new SecurityException(String.format("取消失敗：課程將於 %s 開始，距離不足 %d 小時。",
                    nextCourseTime != null ? nextCourseTime.toString() : "未知時間", ENROLLMENT_CANCEL_CUTOFF_HOURS));
        }
        logger.info("確認報名 ID {} 取消時間符合提前取消時限 (提前 {} 小時)", enrollmentId, ENROLLMENT_CANCEL_CUTOFF_HOURS);
        // 檢查取消的記錄狀態，只有已報名才觸發候補遞補
        boolean wasRegistered = REGISTERED_STATUS.equals(enrollment.getStatus());
        logger.info("報名 ID {} 原狀態為 {}", enrollmentId, enrollment.getStatus());
        // 檢查取消的記錄狀態，如果已經是非活躍狀態，則不能取消
        if (INACTIVE_ENROLLMENT_STATUSES.contains(enrollment.getStatus())) {
             logger.warn("報名 ID {} 狀態 {} 不允許取消。", enrollmentId, enrollment.getStatus());
            throw new IllegalStateException(String.format("報名狀態不正確，無法取消。目前狀態: %s", enrollment.getStatus()));
        }
        // 執行取消操作：將狀態設為 '已取消'
        enrollment.setStatus(CANCELLED_STATUS);
        enrollmentDAO.save(enrollment);
        logger.info("報名 ID {} 狀態更新為 {}。", enrollmentId, CANCELLED_STATUS);
        // 候補名單自動遞補
        // 只有當取消的是 '已報名' 的記錄時，且課程仍有空位時才觸發遞補
        if (wasRegistered) {
             processWaitlist(course.getId());
        }
    }

	// 查詢並返回指定使用者的所有常規課程報名記錄
    @Override
    public List<EnrollmentDTO> getEnrollmentsByUserId(Integer userId) {
        logger.info("查詢使用者 ID {} 的所有常規報名記錄...", userId);
        User user = userDAO.findById(userId)
                .orElseThrow(() -> new EntityNotFoundException("User not found with id: " + userId));
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
        // 確保這是常規課程 (如果需要只返回常規課程的報名記錄)
        if (course.getIsTrial() != null && course.getIsTrial()) {
             logger.warn("嘗試獲取體驗課程 ID {} 的常規報名記錄。", courseId);
             // 可以選擇拋出例外，或者返回空列表
              throw new IllegalStateException("Course ID " + courseId + " is a trial course.");
             // return Collections.emptyList(); // 返回空列表表示沒有常規報名記錄
        }
        List<Enrollment> courseEnrollments = enrollmentDAO.findByCourse(course);
         logger.info("找到常規課程 ID {} 的 {} 條報名記錄。", courseId, courseEnrollments.size());
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
        int enrolledCount = enrollmentDAO.countByCourseAndStatus(course, REGISTERED_STATUS);
        boolean isFull = enrolledCount >= course.getMaxCapacity();
        logger.info("常規課程 ID {} 已報名人數: {}，最大容量: {}，是否已滿: {}",
                   courseId, enrolledCount, course.getMaxCapacity(), isFull);
        return isFull;
    }

	// 檢查指定使用者是否已對特定常規課程進行有效報名或候補
    @Override
    public boolean isUserEnrolled(Integer userId, Integer courseId) {
        logger.info("檢查使用者 ID {} 是否已有效報名或候補常規課程 ID: {}", userId, courseId);
        User user = userDAO.findById(userId)
                .orElseThrow(() -> new EntityNotFoundException("User not found with id: " + userId));
        Course course = courseDAO.findById(courseId)
                .orElseThrow(() -> new EntityNotFoundException("Course not found with id: " + courseId));
        // 修改為檢查是否存在狀態不在非活躍列表中的記錄
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
        // 確保這是常規課程 (如果需要只計算常規課程的人數)
        if (course.getIsTrial() != null && course.getIsTrial()) {
             logger.warn("嘗試獲取體驗課程 ID {} 的已報名人數。", courseId);
             // 可以選擇拋出例外，或者返回 0
              throw new IllegalStateException("Course ID " + courseId + " is a trial course.");
             // return 0; // 體驗課程不適用此計數，返回 0
        }
        int count = enrollmentDAO.countByCourseAndStatus(course, REGISTERED_STATUS);
         logger.info("常規課程 ID {} 的已報名人數: {}", courseId, count);
        return count;
    }

    // 查詢所有常規課程並包含當前使用者的報名狀態和人數
    @Override
    public List<CourseInfoDTO> getAllCoursesWithUserStatus(Integer userId) {
        logger.info("查詢所有常規課程並包含使用者 {} 的報名狀態和人數...", userId != null ? userId : "匿名");
        List<Course> allCourses = courseDAO.findAll();
        // 過濾出常規課程
        List<Course> regularCourses = allCourses.stream()
                                           .filter(course -> course.getIsTrial() == null || !course.getIsTrial())
                                           .collect(Collectors.toList());
        if (regularCourses.isEmpty()) {
             logger.info("沒有找到任何常規課程。");
             return Collections.emptyList();
        }
        logger.info("找到 {} 條常規課程。", regularCourses.size());

        final User finalUser = (userId != null) ? userDAO.findById(userId).orElse(null) : null;
        if (userId != null && finalUser == null) {
             logger.warn("傳入的使用者 ID {} 未找到對應的 User 實體，將無法查詢個人報名狀態。", userId);
        }
        final List<Enrollment> allRelevantEnrollments = (finalUser != null) ?
             enrollmentDAO.findByUser(finalUser).stream()
                 .filter(e -> e.getCourse() != null && regularCourses.contains(e.getCourse()))
                 .collect(Collectors.toList()) :
             Collections.emptyList();

        if (finalUser != null) {
            logger.info("找到使用者 {} 的 {} 條相關報名記錄。", userId, allRelevantEnrollments.size());
        }
        // 一次性獲取所有常規課程中已報名和候補的記錄
        List<Enrollment> allRegisteredEnrollments = enrollmentDAO.findByCourseInAndStatus(regularCourses, REGISTERED_STATUS);
        List<Enrollment> allWaitingEnrollments = enrollmentDAO.findByCourseInAndStatus(regularCourses, WAITING_STATUS);

        final Map<Integer, Long> registeredCountsMap = allRegisteredEnrollments.stream()
             .filter(e -> e.getCourse() != null)
             .collect(Collectors.groupingBy(e -> e.getCourse().getId(), Collectors.counting()));

        final Map<Integer, Long> waitlistCountsMap = allWaitingEnrollments.stream()
             .filter(e -> e.getCourse() != null)
             .collect(Collectors.groupingBy(e -> e.getCourse().getId(), Collectors.counting()));

        return regularCourses.stream().map(course -> {
            // 從 Map 中獲取已報名和候補人數，如果沒有則為 0
             long registeredCount = registeredCountsMap.getOrDefault(course.getId(), 0L);
             long waitlistCount = waitlistCountsMap.getOrDefault(course.getId(), 0L);
            String userStatus = "未報名"; // 預設狀態
            if (finalUser != null) {
                 Optional<Enrollment> currentUserEnrollment = allRelevantEnrollments.stream()
                         .filter(e -> e.getUser() != null && e.getUser().getId().equals(finalUser.getId()) && e.getCourse() != null && e.getCourse().getId().equals(course.getId()))
                         .findFirst();
                 if (currentUserEnrollment.isPresent()) {
                     userStatus = currentUserEnrollment.get().getStatus();
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
                    .registeredCount((int) registeredCount)
                    .waitlistCount((int) waitlistCount)
                    .userStatus(userStatus)
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
        // 確保這是常規課程 (如果需要只為常規課程添加候補)
        if (course.getIsTrial() != null && course.getIsTrial()) {
             logger.warn("嘗試將使用者 ID {} 加入體驗課程 ID {} 的候補名單。", userId, courseId);
             throw new IllegalStateException("該課程為體驗課程，無法加入常規候補名單。");
        }
        logger.info("確認課程 ID {} 是常規課程。", courseId);
        // 檢查使用者是否已存在非取消的報名記錄 (即已報名或候補中或其他活躍狀態)
        // 如果是，檢查具體是已報名還是候補中，並拋出對應的錯誤
        if (enrollmentDAO.existsByUserAndCourseAndStatusNotIn(user, course, List.of(CANCELLED_STATUS, COMPLETED_STATUS, NO_SHOW_STATUS))) { // **更動：排除所有非活躍狀態**
             // 如果存在非取消的報名記錄，需要進一步判斷是哪種狀態
             if (enrollmentDAO.existsByUserAndCourseAndStatus(user, course, WAITING_STATUS)) {
                 logger.warn("使用者 ID {} 已在課程 ID {} 的候補名單中。", user.getId(), course.getId());
                 throw new IllegalStateException("您已在候補名單中");
             }
             if (enrollmentDAO.existsByUserAndCourseAndStatus(user, course, REGISTERED_STATUS)) {
                 logger.warn("使用者 ID {} 已報名課程 ID {}。", user.getId(), course.getId());
                 throw new IllegalStateException("您已報名此課程");
             }
             // 如果還有其他活躍狀態，可以在這裡添加檢查
             logger.warn("使用者 ID {} 已在課程 ID {} 中存在其他活躍狀態的報名記錄。", user.getId(), course.getId());
             throw new IllegalStateException("您已存在其他活躍狀態的報名記錄於此課程。");
        }
         logger.info("使用者 ID {} 於課程 ID {} 不存在活躍報名記錄。", user.getId(), course.getId());

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
         // 確保這是常規課程 (如果需要只處理常規課程的候補)
        if (course.getIsTrial() != null && course.getIsTrial()) {
             logger.warn("嘗試處理體驗課程 ID {} 的候補名單。", courseId);
             // 體驗課程的候補邏輯可能不同，或者不適用此方法
             // throw new IllegalStateException("Course ID " + courseId + " is a trial course.");
             return; // 體驗課程不適用此處理，直接返回
        }
        logger.info("確認課程 ID {} 是常規課程。", courseId);

        long registeredCount = enrollmentDAO.countByCourseAndStatus(course, REGISTERED_STATUS);
        int maxCapacity = course.getMaxCapacity();

        if (registeredCount < maxCapacity) {
            logger.info("課程 ID {} 有空位，已報名人數: {}，最大容量: {}", course.getId(), registeredCount, maxCapacity);
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
                enrollment.setStatus(REGISTERED_STATUS);
                enrollmentDAO.save(enrollment);
                promotedCount++;
                logger.info("候補名單自動遞補：使用者 {} (報名 ID {}) 已從候補遞補為已報名 課程 ID: {}",
                           enrollment.getUser() != null ? enrollment.getUser().getId() : "Unknown User",
                           enrollment.getId(), course.getId());
                // *** 可選：發送通知給被遞補的使用者 (需要額外的通知服務) ***
                // 例如：notificationService.sendPromotionNotification(promotedEnrollment.getUser(), course);
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
        enrollment.setStatus(newStatus);
        Enrollment updatedEnrollmentEntity = enrollmentDAO.save(enrollment);
        logger.info("報名 ID {} 狀態成功從 {} 更新為 {}", enrollmentId, oldStatus, newStatus);
        boolean wasActive = !INACTIVE_ENROLLMENT_STATUSES.contains(oldStatus);
        boolean isNowCancelled = CANCELLED_STATUS.equals(newStatus);

        if (wasActive && isNowCancelled) {
             logger.info("報名 ID {} 狀態從活躍 ({}) 變為已取消，觸發候補遞補流程。", enrollmentId, oldStatus);
             Course course = updatedEnrollmentEntity.getCourse();
             if (course != null) {
                 processWaitlist(course.getId());
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
        int dayOfWeekValue = course.getDayOfWeek();
        if (dayOfWeekValue < 0 || dayOfWeekValue > 6) {
             logger.warn("Course ID {} has invalid dayOfWeek value: {}. Cannot calculate last occurrence.", course.getId(), dayOfWeekValue);
             return null;
        }
        DayOfWeek courseDayOfWeek = DayOfWeek.of(dayOfWeekValue % 7 + 1);

        LocalDate lastDate = relativeDate;
        // 計算當前日期與課程星期幾之間的差距天數
        int daysSinceLast = relativeDate.getDayOfWeek().getValue() - courseDayOfWeek.getValue();

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
}
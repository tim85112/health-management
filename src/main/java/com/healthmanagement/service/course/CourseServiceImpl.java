package com.healthmanagement.service.course;

import com.healthmanagement.dao.course.CourseDAO;
import com.healthmanagement.dao.course.TrialBookingDAO;
import com.healthmanagement.dao.member.UserDAO;
import com.healthmanagement.dto.course.ConvertToDTO;
import com.healthmanagement.dto.course. CourseRequest; // 使用  CourseRequest
import com.healthmanagement.dto.course. CourseResponse; // 使用  CourseResponse
import com.healthmanagement.model.course.Course;
import com.healthmanagement.model.member.User; // 引入 User Entity
import com.healthmanagement.model.course.TrialBooking;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.temporal.ChronoUnit;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.Collection;
import java.util.Collections; // 引入 Collections

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Service
public class CourseServiceImpl implements CourseService {

    private static final Logger logger = LoggerFactory.getLogger(CourseServiceImpl.class);

    // 將依賴宣告為 final，並移除 @Autowired
    private final CourseDAO courseDAO;
    private final TrialBookingDAO trialBookingDAO;
    private final EnrollmentService enrollmentService;
    private final UserDAO userDAO;
    private final ConvertToDTO convertToDTOConverter;
    private final EntityManager entityManager; // EntityManager 可以不是 final

    // 常規和體驗預約的非活躍狀態
    private static final List<String> INACTIVE_ENROLLMENT_STATUSES = List.of("已取消", "已完成", "未到場");
    private static final List<String> INACTIVE_TRIAL_STATUSES = List.of("已取消", "已完成", "未到場");


    // 使用建構子注入所有依賴，並標記 @Autowired
    @Autowired
    public CourseServiceImpl(CourseDAO courseDAO, TrialBookingDAO trialBookingDAO,
                             EnrollmentService enrollmentService, UserDAO userDAO,
                             ConvertToDTO convertToDTOConverter, EntityManager entityManager) {
        this.courseDAO = courseDAO;
        this.trialBookingDAO = trialBookingDAO;
        this.enrollmentService = enrollmentService;
        this.userDAO = userDAO;
        this.convertToDTOConverter = convertToDTOConverter; // 初始化 final 變數
        this.entityManager = entityManager;
    }


    // 輔助方法：將 Course 實體轉換為 CourseResponse DTO
    // 這個方法現在需要知道 bookedTrialCount
    // 注意：這裡應該使用  CourseResponse
    private  CourseResponse convertToCourseResponse(Course course, Integer bookedTrialCount) {
        if (course == null) {
            return null;
        }
        // 確保 coach 對象被載入 (如果需要 coachName)
        // 根據你的 Course Entity @ManyToOne FetchType 或 JPQL Fetch Join 決定是否需要手動載入
        User coach = course.getCoach();
        Integer coachId = (coach != null) ? coach.getId() : null;
        String coachName = (coach != null) ? coach.getName() : "N/A";

        // 使用  CourseResponse
        return  CourseResponse.builder() // 使用 Builder
                .id(course.getId())
                .name(course.getName())
                .description(course.getDescription())
                .coachId(coachId)
                .coachName(coachName)
                .dayOfWeek(course.getDayOfWeek())
                .startTime(course.getStartTime())
                .duration(course.getDuration())
                .maxCapacity(course.getMaxCapacity())
                .offersTrialOption(course.getOffersTrialOption())
                .maxTrialCapacity(course.getMaxTrialCapacity())
                .bookedTrialCount(bookedTrialCount != null ? bookedTrialCount : 0) // 設置計數
                .build();
    }

    // 輔助方法：計算課程的下一個發生日期和時間，相對於當前日期時間
    // 從 EnrollmentService 複製過來並稍作調整 (假設 DayOfWeek 是 0=Sun, 6=Sat)
    private LocalDateTime calculateNextCourseOccurrenceTime(Course course) {
         if (course == null || course.getDayOfWeek() == null || course.getStartTime() == null) {
              logger.warn("課程 ID {} 有不完整的排程資訊 (dayOfWeek: {}, startTime: {})。無法計算下一個發生。",
                          course != null ? course.getId() : "N/A", course != null ? course.getDayOfWeek() : "N/A", course != null ? course.getStartTime() : "N/A");
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


    // 輔助方法：批量獲取提供體驗選項的課程的下一個排程的 bookedTrialCount
    // 這裡複用 EnrollmentService 中的方法
    // 確保 EnrollmentService 已經實現了 getNextOccurrenceBookedTrialCounts
    private Map<Integer, Integer> getNextOccurrenceBookedTrialCounts(List<Course> courses) {
        if (courses == null || courses.isEmpty()) {
            return Collections.emptyMap();
        }

        // 過濾出提供體驗選項的課程
        List<Course> coursesOfferingTrial = courses.stream()
             .filter(course -> course.getOffersTrialOption() != null && course.getOffersTrialOption())
             .collect(Collectors.toList());

        if (coursesOfferingTrial.isEmpty()) {
            return Collections.emptyMap();
        }

        // 調用 EnrollmentService 中已實現的邏輯來獲取 bookedTrialCount
        // 假設 EnrollmentService.getNextOccurrenceBookedTrialCounts() 返回正確結果
        return enrollmentService.getNextOccurrenceBookedTrialCounts(coursesOfferingTrial);
    }


    @Override
    @Transactional(readOnly = true) // 標記為只讀事務
    public List< CourseResponse> getAllCourses() { // 修改返回類型為 List< CourseResponse>
        logger.info("獲取所有課程並轉換為  CourseResponse。");
        List<Course> courses = courseDAO.findAll();

        // 獲取提供體驗選項的課程的下一個排程的 bookedTrialCount
        Map<Integer, Integer> bookedTrialCounts = getNextOccurrenceBookedTrialCounts(courses);


        // 將 Course 實體列表轉換為  CourseResponse 列表，並帶入 bookedTrialCount
        List< CourseResponse> responseList = courses.stream()
             .map(course -> convertToCourseResponse(course, bookedTrialCounts.getOrDefault(course.getId(), 0))) // 從 map 中獲取計數，默認 0
             .collect(Collectors.toList());

        logger.info("返回 {} 個課程作為  CourseResponse。", responseList.size());
        return responseList;
    }

    @Override
    @Transactional(readOnly = true)
    public  CourseResponse getCourseById(Integer id) { // 返回  CourseResponse
        logger.info("獲取課程 ID: {}", id);
        Optional<Course> optional = courseDAO.findById(id);
        if (optional.isEmpty()) {
            logger.warn("找不到課程 ID: {}", id);
            // return null; // 不應該返回 null，應該拋出例外
            throw new EntityNotFoundException("找不到課程 ID: " + id);
        }
        Course course = optional.get();

        // 獲取單個課程的下一個排程的 bookedTrialCount (如果提供體驗選項)
        Integer bookedTrialCount = 0;
        if (course.getOffersTrialOption() != null && course.getOffersTrialOption()) {
            // 創建一個單元素的列表來調用 getNextOccurrenceBookedTrialCounts
            List<Course> singleCourseList = Collections.singletonList(course);
            Map<Integer, Integer> countsMap = getNextOccurrenceBookedTrialCounts(singleCourseList);
            bookedTrialCount = countsMap.getOrDefault(course.getId(), 0);
        }

        return convertToCourseResponse(course, bookedTrialCount); // 傳入計數
    }

    @Override
    @Transactional(readOnly = true)
    public Course getById(Integer id) {
        // 這個方法通常用於內部，返回 Entity，保持不變
        logger.info("獲取課程 Entity ID: {}", id);
        Optional<Course> optional = courseDAO.findById(id);
        if (optional.isEmpty()) {
            logger.warn("找不到課程 Entity ID: {}", id);
            // return null; // 不應該返回 null，應該拋出例外
             throw new EntityNotFoundException("找不到課程 Entity ID: " + id);
        }
        return optional.get();
    }


    @Override
    @Transactional // 標記為事務性方法
    public  CourseResponse createCourse( CourseRequest courseRequest) { // 使用  CourseRequest
        logger.info("創建新課程，名稱: {}", courseRequest.getName());

        // 添加創建課程的業務驗證
        validateCourseRequest(courseRequest);

        // 獲取 Coach 的引用，避免不必要的資料庫查詢
        User coachRef = entityManager.getReference(User.class, courseRequest.getCoachId());
        Course course = new Course();
        course.setName(courseRequest.getName());
        course.setDescription(courseRequest.getDescription());
        course.setDayOfWeek(courseRequest.getDayOfWeek());
        course.setStartTime(courseRequest.getStartTime());
        course.setCoach(coachRef);
        course.setDuration(courseRequest.getDuration());
        course.setMaxCapacity(courseRequest.getMaxCapacity());
        // 從  CourseRequest 設置 offersTrialOption 和 maxTrialCapacity
        course.setOffersTrialOption(courseRequest.getOffersTrialOption() != null ? courseRequest.getOffersTrialOption() : false);
        course.setMaxTrialCapacity(courseRequest.getMaxTrialCapacity());

        Course savedCourse = courseDAO.save(course);
        logger.info("課程創建成功，ID: {}", savedCourse.getId());

        // 新增的課程 bookedTrialCount 為 0
        return convertToCourseResponse(savedCourse, 0); // 新增時計數為 0
    }

    @Override
    @Transactional // 標記為事務性方法
    public  CourseResponse updateCourse(Integer id,  CourseRequest courseRequest) { // 使用  CourseRequest
        logger.info("更新課程 ID: {}", id);
        Optional<Course> optional = courseDAO.findById(id);
        if (optional.isEmpty()) {
            logger.warn("嘗試更新課程 ID {} 但未找到。", id);
            throw new EntityNotFoundException("找不到課程 ID: " + id);
        }
        Course existingCourse = optional.get();

        // 添加更新課程的業務驗證
        validateCourseRequest(courseRequest);

        // TODO: 驗證：如果 maxTrialCapacity 被減少，需要檢查是否小於當前已有的 bookedTrialCount (針對所有未來排程？下一個排程？)
        // 最準確應檢查所有未來排程的體驗預約總數是否超過新的 maxTrialCapacity。這裡暫不實現。

        existingCourse.setName(courseRequest.getName());
        existingCourse.setDescription(courseRequest.getDescription());
        existingCourse.setDayOfWeek(courseRequest.getDayOfWeek());
        existingCourse.setStartTime(courseRequest.getStartTime());
        // 獲取 Coach 的引用
        User coachRef = entityManager.getReference(User.class, courseRequest.getCoachId());
        existingCourse.setCoach(coachRef);
        existingCourse.setDuration(courseRequest.getDuration());
        existingCourse.setMaxCapacity(courseRequest.getMaxCapacity());
        // 從  CourseRequest 更新 offersTrialOption 和 maxTrialCapacity
        existingCourse.setOffersTrialOption(courseRequest.getOffersTrialOption() != null ? courseRequest.getOffersTrialOption() : false);
        existingCourse.setMaxTrialCapacity(courseRequest.getMaxTrialCapacity());


        Course updatedCourse = courseDAO.save(existingCourse);
        logger.info("課程 ID {} 更新成功。", updatedCourse.getId());

        // 獲取更新後的課程下一個排程的 bookedTrialCount (如果提供體驗選項)
         Integer bookedTrialCount = 0;
         if (updatedCourse.getOffersTrialOption() != null && updatedCourse.getOffersTrialOption()) {
            // 創建一個單元素的列表來調用 getNextOccurrenceBookedTrialCounts
            List<Course> singleCourseList = Collections.singletonList(updatedCourse);
            Map<Integer, Integer> countsMap = getNextOccurrenceBookedTrialCounts(singleCourseList);
            bookedTrialCount = countsMap.getOrDefault(updatedCourse.getId(), 0);
         }

        return convertToCourseResponse(updatedCourse, bookedTrialCount); // 傳入計數
    }

    // 輔助方法：驗證  CourseRequest
    private void validateCourseRequest( CourseRequest courseRequest) {
         if (courseRequest.getOffersTrialOption() != null && courseRequest.getOffersTrialOption()) {
             if (courseRequest.getMaxTrialCapacity() == null || courseRequest.getMaxTrialCapacity() <= 0) {
                 throw new IllegalArgumentException("如果提供體驗選項，最大體驗人數必須設定且大於 0。");
             }
             // 確保 maxTrialCapacity 不超過 maxCapacity (可選業務規則)
             if (courseRequest.getMaxTrialCapacity() != null && courseRequest.getMaxTrialCapacity() > courseRequest.getMaxCapacity()) {
                 logger.warn("課程 '{}' 提供體驗，但最大體驗人數 ({}) 大於最大常規報名人數 ({})",
                          courseRequest.getName(), courseRequest.getMaxTrialCapacity(), courseRequest.getMaxCapacity());
                 // 根據需求決定是否拋出異常或只是記錄警告
                 // throw new IllegalArgumentException("最大體驗人數不能超過最大常規報名人數。");
             }
         } else {
             // 如果不提供體驗選項，理論上不應該設定 maxTrialCapacity
             if (courseRequest.getMaxTrialCapacity() != null && courseRequest.getMaxTrialCapacity() > 0) {
                  logger.warn("課程 '{}' 不提供體驗選項，但最大體驗人數設定為 {}。將忽略此值。", courseRequest.getName(), courseRequest.getMaxTrialCapacity());
                  // 將 DTO 中的 maxTrialCapacity 設為 null，以便在 Entity 中儲存 null 或 0
                  courseRequest.setMaxTrialCapacity(null); // 修改 DTO，影響後續 Entity 設置
             }
         }
         // TODO: 添加其他必要的驗證，例如 dayOfWeek 範圍 (0-6), startTime 非空, duration > 0, maxCapacity > 0, coachId 非空 等
         if (courseRequest.getDayOfWeek() == null || courseRequest.getDayOfWeek() < 0 || courseRequest.getDayOfWeek() > 6) {
             throw new IllegalArgumentException("星期幾 (dayOfWeek) 必須在 0 到 6 之間。");
         }
         if (courseRequest.getStartTime() == null) {
              throw new IllegalArgumentException("開始時間 (startTime) 不能為空。");
         }
         if (courseRequest.getDuration() == null || courseRequest.getDuration() <= 0) {
              throw new IllegalArgumentException("時長 (duration) 必須大於 0。");
         }
         if (courseRequest.getMaxCapacity() == null || courseRequest.getMaxCapacity() <= 0) {
              throw new IllegalArgumentException("最大容納人數 (maxCapacity) 必須大於 0。");
         }
         if (courseRequest.getCoachId() == null) {
              throw new IllegalArgumentException("教練 ID (coachId) 不能為空。");
         }
         // 可以選擇驗證 coachId 是否存在 User 表中
         // userDAO.findById(courseRequest.getCoachId()).orElseThrow(() -> new EntityNotFoundException("找不到指定的教練 ID: " + courseRequest.getCoachId()));
    }


    @Override
    @Transactional // 標記為事務性方法
    public void deleteCourse(Integer id) {
        logger.info("刪除課程 ID: {}", id);

        // 檢查是否存在活躍的常規報名
        boolean hasActiveEnrollments = enrollmentService.hasActiveEnrollmentsForCourse(id);
        if (hasActiveEnrollments) {
             logger.warn("課程 ID {} 存在活躍的常規報名記錄，無法刪除。", id);
             throw new IllegalStateException("課程 ID " + id + " 存在活躍的常規報名記錄，無法刪除。");
        }

        // 檢查是否存在活躍的體驗預約
        boolean hasActiveTrialBookings = trialBookingDAO.existsByCourseIdAndBookingStatusNotIn(id, INACTIVE_TRIAL_STATUSES);
        if (hasActiveTrialBookings) {
             logger.warn("課程 ID {} 存在活躍的體驗預約記錄，無法刪除。", id);
             throw new IllegalStateException("課程 ID " + id + " 存在活躍的體驗預約記錄，無法刪除。");
        }

        courseDAO.deleteById(id);
        logger.info("課程 ID {} 已刪除。", id);
    }

    @Override
    @Transactional(readOnly = true)
    public List< CourseResponse> findByCoachId(Integer coachId) { // 返回 List< CourseResponse>
        logger.info("查詢教練 ID {} 的課程。", coachId);
        List<Course> courses = courseDAO.findByCoachId(coachId);
        logger.info("找到教練 ID {} 的 {} 個課程。", coachId, courses.size());

        // 批量獲取提供體驗選項的課程的下一個排程的 bookedTrialCount
        Map<Integer, Integer> bookedTrialCounts = getNextOccurrenceBookedTrialCounts(courses);

        // 轉換並帶入 bookedTrialCount
        return courses.stream()
             .map(course -> convertToCourseResponse(course, bookedTrialCounts.getOrDefault(course.getId(), 0)))
             .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List< CourseResponse> searchCoursesByCourseName(String name) { // 返回 List< CourseResponse>
        logger.info("依課程名稱查詢：{}", name);
        List<Course> courses = courseDAO.findByNameContainingIgnoreCase(name);
        logger.info("找到 {} 個匹配名稱 '{}' 的課程。", courses.size(), name);

        // 批量獲取提供體驗選項的課程的下一個排程的 bookedTrialCount
        Map<Integer, Integer> bookedTrialCounts = getNextOccurrenceBookedTrialCounts(courses);


        // 轉換並帶入 bookedTrialCount
        return courses.stream()
             .map(course -> convertToCourseResponse(course, bookedTrialCounts.getOrDefault(course.getId(), 0)))
             .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List< CourseResponse> searchCoursesByCoachName(String coachName) { // 返回 List< CourseResponse>
        logger.info("依教練名稱查詢：{}", coachName);
         // 這裡調用了 findByCoachNameContainingIgnoreCase，假設這個 DAO 方法存在並能根據 coach 的 name 查詢
        List<Course> courses = courseDAO.findByCoachNameContainingIgnoreCase(coachName);

        logger.info("找到 {} 個匹配教練名稱 '{}' 的課程。", courses.size(), coachName);

        // 批量獲取提供體驗選項的課程的下一個排程的 bookedTrialCount
        Map<Integer, Integer> bookedTrialCounts = getNextOccurrenceBookedTrialCounts(courses);

        // 轉換並帶入 bookedTrialCount
        return courses.stream()
             .map(course -> convertToCourseResponse(course, bookedTrialCounts.getOrDefault(course.getId(), 0)))
             .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List< CourseResponse> getCoursesByDayOfWeek(Integer dayOfWeek) { // 返回 List< CourseResponse>
        logger.info("依星期查詢課程：{}", dayOfWeek);
        List<Course> courses = courseDAO.findByDayOfWeek(dayOfWeek);
        logger.info("找到星期 {} 的 {} 個課程。", dayOfWeek, courses.size());

        // 批量獲取提供體驗選項的課程的下一個排程的 bookedTrialCount
        Map<Integer, Integer> bookedTrialCounts = getNextOccurrenceBookedTrialCounts(courses);

        // 轉換並帶入 bookedTrialCount
        return courses.stream()
             .map(course -> convertToCourseResponse(course, bookedTrialCounts.getOrDefault(course.getId(), 0)))
             .collect(Collectors.toList());
    }

    // 依日期時段查詢課程
    @Override
    @Transactional(readOnly = true)
    public List< CourseResponse> getCoursesByDateTimeRange(LocalDateTime startTime, LocalDateTime endTime) {
        logger.info("依日期時段查詢課程服務：查詢範圍從 {} 到 {}。", startTime, endTime);

        // 1. 從資料庫獲取所有課程
        // 注意：在課程數量非常龐大的情況下，獲取所有課程到記憶體可能存在性能問題。
        // 如果性能成為瓶頸，需要考慮改為資料庫層面的複雜查詢。
        List<Course> allCourses = courseDAO.findAll();
        logger.info("獲取所有 {} 門課程，準備進行日期時段過濾。", allCourses.size());

        // 2. 在 Service 層過濾課程，判斷其是否在給定的日期時間範圍內發生過
        List<Course> filteredCourses = allCourses.stream()
            .filter(course -> doesCourseOccurInRange(course, startTime, endTime))
            .collect(Collectors.toList());

        logger.info("過濾後找到 {} 門在指定日期時段內發生過的課程。", filteredCourses.size());

        // 3. 獲取過濾後課程的下一個排程的 bookedTrialCount (如 CourseInfoDTO 中所示)
        // 這裡複用 EnrollmentService 中的方法
        // 確保 EnrollmentService 已經實現了 getNextOccurrenceBookedTrialCounts
        Map<Integer, Integer> bookedTrialCounts = getNextOccurrenceBookedTrialCounts(filteredCourses);


        // 4. 轉換並帶入 bookedTrialCount
        return filteredCourses.stream()
             .map(course -> convertToCourseResponse(course, bookedTrialCounts.getOrDefault(course.getId(), 0))) // 使用 convertToCourseResponse
             .collect(Collectors.toList());
    }


    // 輔助方法：判斷一個課程在給定的日期時間範圍內是否有發生
    // 根據 Course 的 dayOfWeek 和 startTime 計算其發生日期時間點
    private boolean doesCourseOccurInRange(Course course, LocalDateTime rangeStart, LocalDateTime rangeEnd) {
        // 檢查必要的欄位是否存在且有效
        if (course == null || course.getDayOfWeek() == null || course.getStartTime() == null ||
            rangeStart == null || rangeEnd == null || rangeStart.isAfter(rangeEnd)) {
            logger.warn("驗證發生範圍失敗：課程或範圍參數無效。課程 ID: {}, 星期: {}, 時間: {}, 範圍: {} 到 {}",
                        course != null ? course.getId() : "N/A",
                        course != null ? course.getDayOfWeek() : "N/A",
                        course != null ? course.getStartTime() : "N/A",
                        rangeStart, rangeEnd);
            return false;
        }

        // 獲取課程設定的星期幾 (DayOfWeek enum)
        // 這裡需要確認你的 DayOfWeek 儲存方式與 java.time.DayOfWeek (1=Mon, 7=Sun) 的對應關係
        // 假設你的資料庫是 0=Sun, 6=Sat
        DayOfWeek courseDayOfWeek;
        try {
            // 將 0-6 (Sun-Sat) 轉換為 Java 的 1-7 (Mon-Sun)
            courseDayOfWeek = DayOfWeek.of((course.getDayOfWeek() + 1) % 7 == 0 ? 7 : (course.getDayOfWeek() + 1) % 7);
        } catch (Exception e) {
            logger.warn("課程 ID {} 的星期幾值無效: {}。", course.getId(), course.getDayOfWeek());
            return false; // 無效的星期幾值
        }

        LocalTime courseStartTime = course.getStartTime();

        // 從查詢範圍的開始日期開始檢查
        LocalDate currentCheckDate = rangeStart.toLocalDate();

        // 找到第一個在 rangeStart 日期或之後，且符合課程星期幾的日期
        // TemporalAdjusters.nextOrSame(dayOfWeek) 會找到當前日期或之後的第一個符合 dayOfWeek 的日期
        currentCheckDate = currentCheckDate.with(TemporalAdjusters.nextOrSame(courseDayOfWeek));

        // 將這個日期與課程的開始時間結合，得到一個潛在的發生時間點
        LocalDateTime potentialOccurrence = LocalDateTime.of(currentCheckDate, courseStartTime);

        // 如果這個潛在的發生時間點早於查詢範圍的精確開始時間 (包括時分秒)，則需要檢查下一個星期的同一天
        if (potentialOccurrence.isBefore(rangeStart)) {
            potentialOccurrence = potentialOccurrence.plusWeeks(1);
        }

        // 現在 potentialOccurrence 是在 rangeStart 或之後，且是課程星期幾和開始時間的第一個潛在發生時間點。
        // 檢查這個或之後的發生時間點是否落在查詢範圍 [rangeStart, rangeEnd] 內。
        // 只要找到一個發生點在範圍內，就返回 true。
        while (!potentialOccurrence.isAfter(rangeEnd)) {
            // 如果當前這個潛在發生時間點在範圍 [rangeStart, rangeEnd] 內，則表示課程發生過
            // isBefore(rangeStart) 為 false 且 isAfter(rangeEnd) 為 false <=> 在範圍內或邊界上
            if (!potentialOccurrence.isBefore(rangeStart) && !potentialOccurrence.isAfter(rangeEnd)) {
                return true; // 找到一個在範圍內的發生點
            }

            // 否則，檢查下一個星期的同一時間
            potentialOccurrence = potentialOccurrence.plusWeeks(1);
        }

        // 循環結束，表示在整個範圍內沒有找到任何發生點
        return false;
    }

    // TODO: 如果 EnrollmentService.getNextOccurrenceBookedTrialCounts 尚未實現，則需要自己實現或與 EnrollmentService 協調。
    // 這裡依賴 EnrollmentService 中有這個方法。

}
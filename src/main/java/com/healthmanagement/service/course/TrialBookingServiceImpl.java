package com.healthmanagement.service.course;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.security.access.AccessDeniedException; // 引入 AccessDeniedException，如果需要的話

import com.healthmanagement.dao.course.TrialBookingDAO;
import com.healthmanagement.dao.course.CourseDAO;
import com.healthmanagement.dao.member.UserDAO;

import com.healthmanagement.dto.course.TrialBookingRequestDTO;
import com.healthmanagement.dto.course.ConvertToDTO; // 如果 ConvertToDTO 包含 TrialBooking 相關轉換
import com.healthmanagement.dto.course.TrialBookingDTO;

import com.healthmanagement.model.course.TrialBooking;
import com.healthmanagement.model.course.Course;
import com.healthmanagement.model.member.User;

import jakarta.persistence.EntityNotFoundException;
import java.time.LocalDateTime;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.Collection; // 引入 Collection

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Service
public class TrialBookingServiceImpl implements TrialBookingService {

	private static final Logger logger = LoggerFactory.getLogger(TrialBookingServiceImpl.class);

    @Autowired
    private TrialBookingDAO trialBookingDAO;

    @Autowired
    private UserDAO userDAO;

    @Autowired
    private CourseDAO courseDAO;

    @Autowired
    private EnrollmentService enrollmentService; // 注入 EnrollmentService 用於檢查常規報名

    // 如果 ConvertToDTO 類別包含 TrialBooking 的轉換方法，可以注入；
    // 否則就保留在 Service 內部實現轉換輔助方法。
    // @Autowired
    // private ConvertToDTO convertToDTOConverter;


    // 將硬編碼的訪客使用者 ID 移至配置屬性
    @Value("${app.guest-user-id:2}") // 從 application.properties 讀取，預設值為 2
    private Integer guestUserId;

    private static final String BOOKED_STATUS = "已預約";
    private static final String CANCELLED_STATUS = "已取消";
    private static final String COMPLETED_STATUS = "已完成";
    private static final String NO_SHOW_STATUS = "未到場";

    private static final List<String> ACTIVE_TRIAL_STATUSES = List.of(BOOKED_STATUS);
    private static final List<String> INACTIVE_TRIAL_STATUSES = List.of(CANCELLED_STATUS, COMPLETED_STATUS, NO_SHOW_STATUS);

    // 設定預約檢查的時限（例如提前 24 小時）
    private static final long BOOKING_CUTOFF_HOURS = 24;

    // 檢查預約是否在指定的時限內 (例如 24 小時，針對「預約的具體日期 + 預約的具體時間」與當前時間比較）
    private boolean isWithinHours(LocalDate bookingDate, LocalTime startTime, long hours) {
        if (bookingDate == null || startTime == null) {
            logger.warn("Trial booking date or time is null, cannot perform {}-hour check.", hours);
            return false;
        }
        LocalDateTime bookingDateTime = LocalDateTime.of(bookingDate, startTime);
        LocalDateTime now = LocalDateTime.now();
        // 檢查預約時間是否在未來 (必須晚於當前時間)
        if (!bookingDateTime.isAfter(now)) {
             return false; // 預約時間已過或就是現在，不符合 within future hours 條件
        }
        // 檢查距離是否小於指定小時數 (即是否在截止時間之後)
        long hoursBetween = ChronoUnit.HOURS.between(now, bookingDateTime);
        return hoursBetween < hours;
    }

    // 輔助方法：將 TrialBooking 實體轉換為 TrialBookingDTO
    // 如果 ConvertToDTO 類別有此方法，可以刪除此處並使用 ConvertToDTO
    private TrialBookingDTO convertToTrialBookingDTO(TrialBooking trialBooking) {
        if (trialBooking == null) {
            return null;
        }
        TrialBookingDTO.TrialBookingDTOBuilder builder = TrialBookingDTO.builder()
                .id(trialBooking.getId())
                .userId(trialBooking.getUser() != null ? trialBooking.getUser().getId() : null)
                .courseId(trialBooking.getCourse() != null ? trialBooking.getCourse().getId() : null)
                .bookingName(trialBooking.getBookingName())
                .bookingPhone(trialBooking.getBookingPhone())
                .bookingDate(trialBooking.getBookingDate())
                .startTime(trialBooking.getCourse() != null ? trialBooking.getCourse().getStartTime() : null)
                .bookingStatus(trialBooking.getBookingStatus())
                .bookedAt(trialBooking.getBookedAt());
        if (trialBooking.getUser() != null) {
            builder.userName(trialBooking.getUser().getName());
        }
        if (trialBooking.getCourse() != null) {
             builder.courseName(trialBooking.getCourse().getName());
             if (trialBooking.getCourse().getCoach() != null) {
                 builder.coachName(trialBooking.getCourse().getCoach().getName());
             }
        }
        return builder.build();
    }

    // 預約體驗課程
    @Transactional
    @Override
    public TrialBookingDTO bookTrialCourse(Integer userId, TrialBookingRequestDTO bookingRequestDTO) {
        User user = null;
        if (userId != null) {
            user = userDAO.findById(userId)
                    .orElseThrow(() -> new EntityNotFoundException("找不到使用者 ID: " + userId));
            logger.info("已認證用戶 (ID: {}) 嘗試預約體驗課程...", userId);
        } else {
            user = userDAO.findById(guestUserId)
                    .orElseThrow(() -> new EntityNotFoundException("找不到訪客使用者帳號，請確認訪客帳號已創建且 ID 正確。配置的訪客 ID: " + guestUserId));
            logger.info("訪客用戶 (ID: {}) 嘗試預約體驗課程...", guestUserId);
        }
        Course course = courseDAO.findById(bookingRequestDTO.getCourseId())
                .orElseThrow(() -> new EntityNotFoundException("找不到課程 ID: " + bookingRequestDTO.getCourseId()));

        logger.info("預約處理中，課程 ID {} (offersTrialOption: {}).", course.getId(), course.getOffersTrialOption());

        // *** MODIFICATION: 檢查課程是否提供體驗選項 ***
        if (course.getOffersTrialOption() == null || !course.getOffersTrialOption()) {
            logger.warn("使用者 ID {} 嘗試預約不提供體驗選項的課程 ID {}.", userId != null ? userId : user.getId(), course.getId());
            throw new IllegalStateException("此課程不提供體驗預約。");
        }
        logger.info("課程 ID {} 提供體驗選項，繼續預約流程。", course.getId());


        // 檢查使用者是否已報名常規課程
        // 只有未報名常規課程的使用者才能預約體驗 (這個業務規則保留)
        if (userId != null && enrollmentService.isUserEnrolled(userId, course.getId())) {
             logger.warn("使用者 ID {} 已是常規課程 ID {} 的學員，無需預約體驗課。", userId, course.getId());
             throw new IllegalStateException("您已是此課程的常規學員，無需預約體驗課。");
        }

        // 檢查預約時間是否在指定時限外 (例如必須提前 24 小時)
        // 這裡假設預約必須在課程開始前至少 24 小時
        if (isWithinHours(bookingRequestDTO.getBookingDate(), course.getStartTime(), BOOKING_CUTOFF_HOURS)) {
             // 檢查預約時間是否已經過去
            LocalDateTime bookingDateTime = LocalDateTime.of(bookingRequestDTO.getBookingDate(), course.getStartTime());
            if (bookingDateTime.isBefore(LocalDateTime.now())) {
                 logger.warn("使用者 ID {} 預約課程 ID {} 失敗：預約時間 {} 已過期。",
                          userId != null ? userId : user.getId(), course.getId(), bookingDateTime);
                 throw new IllegalStateException("預約失敗：預約時間已過期。");
            } else {
                logger.warn("使用者 ID {} 預約課程 ID {} 失敗：課程將於 {} 開始，距離不足 {} 小時。",
                         userId != null ? userId : user.getId(), course.getId(), bookingDateTime, BOOKING_CUTOFF_HOURS);
                throw new IllegalStateException(String.format("報名失敗：課程將於 %s 開始，距離不足 %d 小時。",
                                    bookingDateTime.toString(), BOOKING_CUTOFF_HOURS));
            }
        }
        logger.info("確認預約時間符合提前預約時限 (提前 {} 小時)", BOOKING_CUTOFF_HOURS);

        String formattedBookingPhone = formatPhoneNumber(bookingRequestDTO.getBookingPhone());
        logger.debug("Formatted phone number for duplicate check: {}", formattedBookingPhone);

        // 檢查是否已預約過 (排除非活躍狀態的預約)
        Optional<TrialBooking> existingActiveBooking;
        // MODIFICATION: 使用 TrialBookingDAO 中新添加的檢查用戶是否存在活躍預約的方法 (根據用戶和課程)
        // 這比之前按日期+電話+姓名檢查更準確，特別是對於登入用戶
        if (userId != null) {
            logger.info("檢查已認證用戶 (ID: {}) 是否已存在課程 ID {} 的活躍體驗預約...", userId, course.getId());
            // 這裡檢查的是是否有任何未來的活躍預約，如果一個用戶只能預約一次體驗課，這個檢查就夠了
            // *** 移除 (Collection<String>) 的強制類型轉換 ***
            existingActiveBooking = trialBookingDAO.findFirstActiveBookingByUserAndCourseAfterDate(
                user, course, INACTIVE_TRIAL_STATUSES, LocalDate.now() // <-- 直接傳入 List<String>
            );
            if (existingActiveBooking.isPresent()) {
                 logger.warn("使用者 ID {} 已存在課程 ID {} 的活躍體驗預約 (ID: {})。",
                          userId, course.getId(), existingActiveBooking.get().getId());
                 throw new IllegalStateException("您已預約過此課程的體驗課。");
            }
            logger.info("已認證用戶 (ID: {}) 在課程 ID {} 不存在活躍體驗預約。", userId, course.getId());
        } else {
             // 對於訪客，仍然使用原有的基於姓名、電話、日期的檢查，以防重複預約
             logger.info("檢查訪客用戶 (ID: {}, Name: {}, Phone: {}) 是否已預約過課程 ID {} 在 {} 的體驗課...",
                       user.getId(), bookingRequestDTO.getBookingName(), bookingRequestDTO.getBookingPhone(), course.getId(), bookingRequestDTO.getBookingDate());
            existingActiveBooking = trialBookingDAO.findByUserAndBookingNameAndBookingPhoneAndCourseAndBookingDateAndBookingStatusNotIn(
                    user,
                    bookingRequestDTO.getBookingName(),
                    formattedBookingPhone,
                    course,
                    bookingRequestDTO.getBookingDate(),
                    INACTIVE_TRIAL_STATUSES
            );
            if (existingActiveBooking.isPresent()) {
                 logger.warn("訪客用戶 (ID: {}, Name: {}, Phone: {}) 已預約過課程 ID {} 在 {} 的體驗課。",
                      user.getId(), bookingRequestDTO.getBookingName(), bookingRequestDTO.getBookingPhone(),
                      course.getId(), bookingRequestDTO.getBookingDate());
                throw new IllegalStateException("您已預約過此課程在該日期的體驗課。");
            }
             logger.info("訪客用戶 (ID: {}, Name: {}, Phone: {}) 在課程 ID {} 在 {} 不存在活躍體驗預約。",
                         user.getId(), bookingRequestDTO.getBookingName(), bookingRequestDTO.getBookingPhone(),
                         course.getId(), bookingRequestDTO.getBookingDate());
        }


        // 容量檢查：計算特定日期和時間點的活躍預約人數，並與 maxTrialCapacity 比較
        long currentTrialBookingCount = trialBookingDAO.countTrialBookingsByCourseDateActualStartTimeAndStatusNotInNative(
            course.getId(),
            bookingRequestDTO.getBookingDate(),
            course.getStartTime(),
            INACTIVE_TRIAL_STATUSES
        );
        logger.info("當前課程 ID {} 在 {} {} 的預約人數 (排除取消/未到場): {}",
                    course.getId(), bookingRequestDTO.getBookingDate(), course.getStartTime(), currentTrialBookingCount);

        Integer maxTrialCapacity = course.getMaxTrialCapacity();

        // MODIFICATION: 檢查 maxTrialCapacity 是否已設定且有效 (>0)
        if (maxTrialCapacity == null || maxTrialCapacity <= 0) {
             logger.warn("課程 ID {} 提供體驗選項，但 maxTrialCapacity 無效或未設定 ({}).", course.getId(), maxTrialCapacity);
             // 這裡可以根據業務規則決定是否允許無限預約或拋出錯誤
             // 如果不允許無限預約，可以拋出錯誤
             throw new IllegalStateException("該體驗課程未設定有效的最大體驗人數限制。");
             // 如果允許無限預約，則跳過下面的容量檢查
        }

        // 檢查容量是否已滿
        if (currentTrialBookingCount >= maxTrialCapacity) {
            logger.warn("預約課程 ID {} 在 {} {} 失敗：預約人數已滿 ({} / {})。",
                       course.getId(), bookingRequestDTO.getBookingDate(), course.getStartTime(), currentTrialBookingCount, maxTrialCapacity);
            throw new IllegalStateException("預約失敗：該體驗課程預約人數已滿。");
        }
        logger.info("容量檢查通過。最大體驗容量: {}", maxTrialCapacity);

        TrialBooking trialBooking = TrialBooking.builder()
                .user(user) // 關聯使用者實體
                .course(course) // 關聯課程實體
                // *** MODIFICATION START: 根據是否為登入用戶來設定預約名字 ***
                // 如果 userId 不為 null (登入用戶)，使用 user.getName()；否則 (訪客)，使用 DTO 中的名字。
                .bookingName(userId != null ? user.getName() : bookingRequestDTO.getBookingName())
                // *** MODIFICATION END ***
                .bookingPhone(formattedBookingPhone) // 確保電話號碼格式化
                .bookingDate(bookingRequestDTO.getBookingDate())
                .bookingStatus(BOOKED_STATUS) // 設定初始狀態為已預約
                .build();

        TrialBooking savedBooking = trialBookingDAO.save(trialBooking);
        logger.info("體驗預約創建成功 (ID: {})", savedBooking.getId());

        // 確認 convertToTrialBookingDTO 方法能正確將 bookingName 包含在 DTO 中
        return convertToTrialBookingDTO(savedBooking);
    }

    // 查詢特定使用者的所有體驗預約
    @Override
    public List<TrialBookingDTO> getTrialBookingsByUserId(Integer userId) {
        logger.info("查詢使用者 ID {} 的所有體驗預約記錄...", userId);
        User user = userDAO.findById(userId)
                .orElseThrow(() -> new EntityNotFoundException("找不到使用者 ID: " + userId));

        List<TrialBooking> userBookings = trialBookingDAO.findByUser(user);
        logger.info("找到使用者 ID {} 的 {} 條體驗預約記錄。", userId, userBookings.size());
        return userBookings.stream()
                           .map(this::convertToTrialBookingDTO)
                           .collect(Collectors.toList());
    }

    // 查詢特定課程的所有體驗預約
    @Override
    public List<TrialBookingDTO> getTrialBookingsByCourseId(Integer courseId) {
        logger.info("查詢課程 {} 的所有體驗預約...", courseId);
        Course course = courseDAO.findById(courseId)
                .orElseThrow(() -> new EntityNotFoundException("找不到課程 ID: " + courseId));

        List<TrialBooking> courseBookings = trialBookingDAO.findByCourse(course);
        logger.info("找到課程 ID {} 的 {} 條預約記錄。", courseId, courseBookings.size());
        return courseBookings.stream()
                .map(this::convertToTrialBookingDTO)
                .collect(Collectors.toList());
    }

	// 取消體驗預約
    @Transactional
    @Override
    public void cancelTrialBooking(Integer bookingId) {
        logger.info("嘗試取消體驗預約 ID: {}", bookingId);
        TrialBooking trialBooking = trialBookingDAO.findById(bookingId)
                .orElseThrow(() -> new EntityNotFoundException("找不到體驗預約 ID: " + bookingId));
        // 確保預約是可以被取消的狀態 (例如不能取消已完成或已過期的)
        if (INACTIVE_TRIAL_STATUSES.contains(trialBooking.getBookingStatus())) {
            logger.warn("預約 ID {} 狀態不正確，無法取消。目前狀態: {}", bookingId, trialBooking.getBookingStatus());
            throw new IllegalStateException(String.format("預約狀態不正確，無法取消。目前狀態: %s", trialBooking.getBookingStatus()));
        }
        // TODO: 可選：添加取消時限檢查，類似於常規報名取消

        trialBooking.setBookingStatus(CANCELLED_STATUS);
        trialBookingDAO.save(trialBooking);
        logger.info("體驗預約 ID {} 取消成功。", bookingId);
    }

    // 查詢單個體驗預約詳細信息
    @Override
    public TrialBookingDTO getTrialBookingDetails(Integer bookingId) {
        logger.info("查詢體驗預約 ID: {} 詳細資訊...", bookingId);
        TrialBooking trialBooking = trialBookingDAO.findById(bookingId)
                .orElseThrow(() -> new EntityNotFoundException("找不到體驗預約 ID: " + bookingId));
        logger.info("找到體驗預約 ID: {}", bookingId);
        return convertToTrialBookingDTO(trialBooking);
    }

    // 排程任務 - 處理過期的體驗預約記錄
    // 每天凌晨 2 點 5 分運行 (確保在 Enrollment 排程後運行)
    @Scheduled(cron = "0 5 2 * * ?")
    @Transactional
    public void processPastDueTrialBookings() {
        logger.info("Running scheduled task: Processing past due trial bookings...");
        LocalDateTime now = LocalDateTime.now(); // 使用當前時間來判斷是否過期
        LocalDate currentDate = now.toLocalDate();
        LocalTime currentTime = now.toLocalTime();
        // 查找所有狀態為 '已預約' 且預約日期時間已過期的記錄
        List<TrialBooking> pastDueBookings = trialBookingDAO.findPastDueBookedTrialBookings(
                BOOKED_STATUS,
                currentDate,
                currentTime
        );
        if (pastDueBookings.isEmpty()) {
            logger.info("No past due trial bookings found to process.");
            return;
        }
        logger.info("Found {} past due trial bookings to process.", pastDueBookings.size());

        int updatedCount = 0;
        for (TrialBooking booking : pastDueBookings) {
            // 將狀態標記為「未到場」
            // 註：這裡假設只要過期且狀態是 BOOKED_STATUS 就標記為未到場。
            // 如果有手動標記「已完成」的功能，手動標記會將狀態從 BOOKED_STATUS 改為 COMPLETED_STATUS，
            // 這樣這些記錄就不會被 findPastDueBookedTrialBookings 查詢到，從而實現手動優先。
            booking.setBookingStatus(NO_SHOW_STATUS);
            trialBookingDAO.save(booking); // 保存更新
            updatedCount++;
            logger.info("Marked Trial Booking ID {} for Course {} on {} {} as {}.",
                       booking.getId(), booking.getCourse() != null ? booking.getCourse().getName() : "Unknown Course",
                       booking.getBookingDate(), booking.getCourse() != null ? booking.getCourse().getStartTime() : "Unknown Time",
                       NO_SHOW_STATUS);
        }
        logger.info("Finished processing past due trial bookings. Updated: {}", updatedCount);
    }

    // 手動更新狀態方法
    @Transactional
    @Override
    public TrialBookingDTO updateBookingStatus(Integer bookingId, String newStatus) {
        logger.info("嘗試手動更新體驗預約 ID {} 的狀態為 {}", bookingId, newStatus);
        // 找要更新的預約記錄
        TrialBooking trialBooking = trialBookingDAO.findById(bookingId)
                .orElseThrow(() -> new EntityNotFoundException("找不到體驗預約 ID: " + bookingId));
        // 驗證傳入的新狀態是否有效
        // 這裡假設您只允許更改為某些特定狀態
        List<String> validManualUpdateStatuses = List.of(BOOKED_STATUS, CANCELLED_STATUS, COMPLETED_STATUS, NO_SHOW_STATUS); // 列出所有允許的狀態
        if (!validManualUpdateStatuses.contains(newStatus)) {
            // 如果傳入的狀態不在允許的列表中，拋出異常
             logger.warn("嘗試將預約 {} 更新為無效狀態: {}", bookingId, newStatus);
            throw new IllegalArgumentException("無效的體驗預約目標狀態: " + newStatus);
        }
        // 檢查當前狀態是否允許轉換到新狀態
        String currentStatus = trialBooking.getBookingStatus();
        // 範例規則：不能更改已經是「已取消」、「已完成」或「未到場」狀態的預約
        if (INACTIVE_TRIAL_STATUSES.contains(currentStatus)) {
            logger.warn("無法從目前狀態 '{}' 更改預約 {} 的狀態。", currentStatus, bookingId);
            throw new IllegalStateException(String.format("無法從目前狀態 '%s' 更改預約狀態。", currentStatus));
        }
        trialBooking.setBookingStatus(newStatus);
        TrialBooking updatedBooking = trialBookingDAO.save(trialBooking);
        logger.info("體驗預約 ID {} 狀態更新為 {} 成功。", bookingId, newStatus);
        return convertToTrialBookingDTO(updatedBooking);
    }

    private String formatPhoneNumber(String rawPhoneNumber) {
        if (rawPhoneNumber == null) {
            return null;
        }
        // 移除所有非數字字元
        String digitsOnly = rawPhoneNumber.replaceAll("\\D", "");
        // 簡單驗證：檢查是否是 10 位數字且以 "09" 開頭 (假設台灣手機號碼格式)
        // 如果驗證不通過，可以選擇拋出例外、記錄警告、返回原始清理後的數字等。
        // 這裡選擇記錄警告並返回清理後的數字（不進行格式化）
        if (digitsOnly.length() != 10 || !digitsOnly.startsWith("09")) {
            logger.warn("Received phone number in unexpected format or invalid: {}. Cleaned digits: {}", rawPhoneNumber, digitsOnly);
            // 如果不是有效的台灣手機號碼格式，不進行自動格式化為 09XX-XXXXXX，返回清理後的數字
            return digitsOnly;
        }
        // 套用 09XX-XXXXXXXX 格式
        // digitsOnly 應該是 10 位數字，例如 "0988888888"
        return digitsOnly.substring(0, 4) + "-" + digitsOnly.substring(4);
    }
}
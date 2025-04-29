package com.healthmanagement.service.course;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.security.access.AccessDeniedException; // 如果需要，引入 AccessDeniedException

import com.healthmanagement.dao.course.TrialBookingDAO;
import com.healthmanagement.dao.course.CourseDAO;
import com.healthmanagement.dao.member.UserDAO; // 需要 UserDAO 來根據 ID 查找用戶（如果需要在其他方法中這樣做）

import com.healthmanagement.dto.course.TrialBookingRequestDTO;
import com.healthmanagement.dto.course.ConvertToDTO; // 如果 ConvertToDTO 包含 TrialBooking 相關轉換
import com.healthmanagement.dto.course.TrialBookingDTO; // 確保 TrialBookingDTO 已新增 email 欄位

import com.healthmanagement.model.course.TrialBooking;
import com.healthmanagement.model.course.Course;
import com.healthmanagement.model.member.User; // 確保引入 User 實體

import jakarta.persistence.EntityNotFoundException;
import java.time.LocalDateTime;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.Collection;
import java.util.Collections;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Service
public class TrialBookingServiceImpl implements TrialBookingService {

	private static final Logger logger = LoggerFactory.getLogger(TrialBookingServiceImpl.class);

    @Autowired
    private TrialBookingDAO trialBookingDAO;

    @Autowired
    private UserDAO userDAO; // 可能需要用來查找用戶，如果方法直接接收 ID

    @Autowired
    private CourseDAO courseDAO;

    @Autowired
    private EnrollmentService enrollmentService; // 注入 EnrollmentService 用於檢查常規報名

    // 如果 ConvertToDTO 類別包含 TrialBooking 的轉換方法，可以注入；
    // 否則就保留在 Service 內部實現轉換輔助方法。
    // @Autowired
    // private ConvertToDTO convertToDTOConverter;


    // 移除非必要了，Controller 會直接傳 User 或 null
    // @Value("${app.guest-user-id:2}")
    // private Integer guestUserId;

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

    private TrialBookingDTO convertToTrialBookingDTO(TrialBooking trialBooking) {
        if (trialBooking == null) {
            return null;
        }
        TrialBookingDTO.TrialBookingDTOBuilder builder = TrialBookingDTO.builder()
                    .id(trialBooking.getId())
                    .userId(trialBooking.getUser() != null ? trialBooking.getUser().getId() : null)
                    .courseId(trialBooking.getCourse() != null ? trialBooking.getCourse().getId() : null)
                    .bookingName(trialBooking.getBookingName())
                    .bookingEmail(trialBooking.getBookingEmail()) // 使用新的屬性名稱
                    .bookingPhone(trialBooking.getBookingPhone())
                    // 注意：這裡不再直接映射 bookingDate 和 startTime 到同名屬性，因為我們需要組合它們給 bookingTime
                    // .bookingDate(trialBooking.getBookingDate()) // <-- 如果 DTO 中有同名字段，可以保留，但要確保語義不混淆
                    // .startTime(trialBooking.getCourse() != null ? trialBooking.getCourse().getStartTime() : null) // <-- 同上
                    .bookingStatus(trialBooking.getBookingStatus())
                    .bookedAt(trialBooking.getBookedAt()); // 預約建立時間

        if (trialBooking.getUser() != null) {
            builder.userName(trialBooking.getUser().getName());
        }

        // 獲取課程和教練信息
        LocalDate bookingDate = trialBooking.getBookingDate(); // 從 Entity 獲取預約日期 (假設在 TrialBooking Entity 上)
        LocalTime startTime = null; // 從 Entity 獲取開始時間 (假設在 Course Entity 上)
        String courseName = null;
        String coachName = null;


        if (trialBooking.getCourse() != null) {
            courseName = trialBooking.getCourse().getName(); // 從關聯課程獲取名稱
            startTime = trialBooking.getCourse().getStartTime(); // 從關聯課程獲取開始時間

            if (trialBooking.getCourse().getCoach() != null) {
                coachName = trialBooking.getCourse().getCoach().getName(); // 從關聯教練獲取名字
            }
        }

        builder.courseName(courseName); // 設定課程名稱
        builder.coachName(coachName);   // 設定教練名稱


        // *** 關鍵修改：組合 bookingDate 和 startTime，設定給 bookingTime ***
        if (bookingDate != null && startTime != null) {
            // 將 LocalDate 和 LocalTime 組合成 LocalDateTime
            builder.bookingTime(LocalDateTime.of(bookingDate, startTime));
        } else {
            // 如果日期或時間為 null，bookingTime 保持為 null
            builder.bookingTime(null); // 顯式設定為 null 也可以，但 builder 預設就是 null
        }
         // *** 修改結束 ***


        return builder.build();
    }

    // 預約體驗課程
    @Transactional
    @Override
    public TrialBookingDTO bookTrialCourse(User user, String contactEmail, TrialBookingRequestDTO bookingRequestDTO) {

        if (user != null) {
            logger.info("已認證用戶 (ID: {}) 嘗試預約體驗課程...", user.getId());
        } else {
             // 匿名使用者，Controller 已處理驗證 email
            logger.info("匿名用戶嘗試預約體驗課程，使用郵箱: {}", contactEmail);
        }

        Course course = courseDAO.findById(bookingRequestDTO.getCourseId())
                .orElseThrow(() -> new EntityNotFoundException("找不到課程 ID: " + bookingRequestDTO.getCourseId()));

        logger.info("預約處理中，課程 ID {} (offersTrialOption: {}).", course.getId(), course.getOffersTrialOption());

        // 檢查課程是否提供體驗選項
        if (course.getOffersTrialOption() == null || !course.getOffersTrialOption()) {
            logger.warn("使用者 {} 嘗試預約不提供體驗選項的課程 ID {}.", user != null ? "ID=" + user.getId() : "匿名", course.getId());
            throw new IllegalStateException("此課程不提供體驗預約。");
        }
        logger.info("課程 ID {} 提供體驗選項，繼續預約流程。", course.getId());

        // 檢查使用者是否已報名常規課程 (僅對登入使用者檢查)
        if (user != null && enrollmentService.isUserEnrolled(user.getId(), course.getId())) {
            logger.warn("使用者 ID {} 已是常規課程 ID {} 的學員，無需預約體驗課。", user.getId(), course.getId());
            throw new IllegalStateException("您已是此課程的常規學員，無需預約體驗課。");
        }
        logger.info("使用者常規課程報名檢查通過。");

        // 檢查預約時間是否在指定時限外 (例如必須提前 24 小時)
        LocalDateTime bookingDateTime = LocalDateTime.of(bookingRequestDTO.getBookingDate(), course.getStartTime());
        if (!bookingDateTime.isAfter(LocalDateTime.now().plusHours(BOOKING_CUTOFF_HOURS))) {
             logger.warn("使用者 {} 預約課程 ID {} 失敗：課程將於 {} 開始，距離不足 {} 小時。",
                         user != null ? "ID=" + user.getId() : "匿名", course.getId(), bookingDateTime, BOOKING_CUTOFF_HOURS);
             throw new IllegalStateException(String.format("報名失敗：預約時間需距離課程開始至少 %d 小時。", BOOKING_CUTOFF_HOURS));
        }
        logger.info("確認預約時間符合提前預約時限 (提前 {} 小時)", BOOKING_CUTOFF_HOURS);


        String formattedBookingPhone = formatPhoneNumber(bookingRequestDTO.getBookingPhone());
        logger.debug("Formatted phone number for duplicate check: {}", formattedBookingPhone);

        // 檢查是否已預約過 (排除非活躍狀態的預約)
        Optional<TrialBooking> existingActiveBooking;

        if (user != null) {
            logger.info("檢查已認證用戶 (ID: {}) 是否已存在課程 ID {} 在 {} 之後的活躍體驗預約...",
                       user.getId(), course.getId(), LocalDate.now());
            existingActiveBooking = trialBookingDAO.findFirstByUserAndCourseAndBookingDateAfterAndStatusNotIn(
                user, // 傳入 User 實體
                course,
                INACTIVE_TRIAL_STATUSES, // 狀態排除
                LocalDate.now() // 從今天開始檢查
            );

            if (existingActiveBooking.isPresent()) {
                logger.warn("使用者 ID {} 已存在課程 ID {} 的活躍體驗預約 (ID: {})。",
                            user.getId(), course.getId(), existingActiveBooking.get().getId());
                throw new IllegalStateException("您已預約過此課程的體驗課。");
            }
            logger.info("已認證用戶 (ID: {}) 在課程 ID {} 不存在活躍體驗預約。", user.getId(), course.getId());

        } else {
            // 呼叫基於 email 的 DAO 方法，名稱需要修改以反映 bookingEmail
            logger.info("檢查匿名用戶 (Email: {}, Name: {}, Phone: {}) 是否已預約過課程 ID {} 在 {} 的體驗課...",
                       contactEmail, bookingRequestDTO.getBookingName(), bookingRequestDTO.getBookingPhone(), course.getId(), bookingRequestDTO.getBookingDate());

            // *** MODIFICATION: 呼叫修改名稱後的 DAO 方法，並傳入 contactEmail ***
            existingActiveBooking = trialBookingDAO.findActiveBookingByBookingEmailAndBookingNameAndBookingPhoneAndCourseAndBookingDateAndBookingStatusNotIn(
                contactEmail, // 使用傳入的 contactEmail
                bookingRequestDTO.getBookingName(),
                formattedBookingPhone,
                course,
                bookingRequestDTO.getBookingDate(),
                INACTIVE_TRIAL_STATUSES
            );

            if (existingActiveBooking.isPresent()) {
                logger.warn("匿名用戶 (Email: {}, Name: {}, Phone: {}) 已預約過課程 ID {} 在 {} 的體驗課。",
                     contactEmail, bookingRequestDTO.getBookingName(), bookingRequestDTO.getBookingPhone(),
                     course.getId(), bookingRequestDTO.getBookingDate());
                throw new IllegalStateException("您已預約過此課程在該日期的體驗課。");
            }
            logger.info("匿名用戶 (Email: {}, Name: {}, Phone: {}) 在課程 ID {} 在 {} 不存在活躍體驗預約。",
                       contactEmail, bookingRequestDTO.getBookingName(), bookingRequestDTO.getBookingPhone(),
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

        // 檢查 maxTrialCapacity 是否已設定且有效 (>0)
        if (maxTrialCapacity == null || maxTrialCapacity <= 0) {
            logger.warn("課程 ID {} 提供體驗選項，但 maxTrialCapacity 無效或未設定 ({}).", course.getId(), maxTrialCapacity);
            throw new IllegalStateException("該體驗課程未設定有效的最大體驗人數限制。");
        }

        // 檢查容量是否已滿
        if (currentTrialBookingCount >= maxTrialCapacity) {
            logger.warn("預約課程 ID {} 在 {} {} 失敗：預約人數已滿 ({} / {})。",
                         course.getId(), bookingRequestDTO.getBookingDate(), course.getStartTime(), currentTrialBookingCount, maxTrialCapacity);
            throw new IllegalStateException("預約失敗：該體驗課程預約人數已滿。");
        }
        logger.info("容量檢查通過。最大體驗容量: {}", maxTrialCapacity);

        // *** 建立 TrialBooking 實體，設定 bookingEmail ***
        TrialBooking trialBooking = TrialBooking.builder()
                .user(user) // 使用傳入的 user 物件 (可能為 null)
                .course(course) // 關聯課程實體
                // 根據 user 是否為 null 來設定預約名字：已登入用用戶名字，匿名用 DTO 名字
                .bookingName(user != null ? user.getName() : bookingRequestDTO.getBookingName())
                .bookingEmail(contactEmail) // *** MODIFICATION: 設定 bookingEmail 欄位 ***
                .bookingPhone(formattedBookingPhone) // 確保電話號碼格式化
                .bookingDate(bookingRequestDTO.getBookingDate())
                .bookingStatus(BOOKED_STATUS) // 設定初始狀態為已預約
                // bookedAt 通常讓資料庫自動生成，這裡不需要設置
                .build();

        TrialBooking savedBooking = trialBookingDAO.save(trialBooking);
        logger.info("體驗預約創建成功 (ID: {})", savedBooking.getId());

        // 確認 convertToTrialBookingDTO 方法能正確將所有欄位（包括 bookingEmail）包含在 DTO 中
        return convertToTrialBookingDTO(savedBooking);
    }

    // 查詢特定使用者的所有體驗預約
    // MODIFICATION: 這個方法接收 userId，如果 user_id 在 DB 是 NULL，它不會被查到。
    // 如果你想讓管理員查所有（包括匿名）或者查匿名預約，需要另外寫方法。
    @Override
    public List<TrialBookingDTO> getTrialBookingsByUserId(Integer userId) {
        logger.info("查詢使用者 ID {} 的所有體驗預約記錄...", userId);
        // 確保傳入的 userId 不為 null，因為我們查找的是與特定用戶關聯的記錄
        if (userId == null) {
            logger.warn("嘗試使用 null userId 查詢體驗預約記錄。");
            return Collections.emptyList(); // 或拋出 IllegalArgumentException
        }
        User user = userDAO.findById(userId)
                .orElseThrow(() -> new EntityNotFoundException("找不到使用者 ID: " + userId));

        // TrialBookingDAO 需要一個 findByUser(User user) 或 findByUserId(Integer userId) 方法
        List<TrialBooking> userBookings = trialBookingDAO.findByUser(user); // 或 userDAO.findByUserId(userId)
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

        // TrialBookingDAO 需要一個 findByCourse(Course course) 或 findByCourseId(Integer courseId) 方法
        List<TrialBooking> courseBookings = trialBookingDAO.findByCourse(course); // 或 trialBookingDAO.findByCourseId(courseId)
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
        // TrialBookingDAO 需要一個方法來查找過期的 BOOKED 狀態的預約
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

    // 電話號碼格式化輔助方法
    private String formatPhoneNumber(String rawPhoneNumber) {
        // 您的格式化邏輯保持不變
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

    // 獲取所有體驗預約紀錄 (支援分頁和篩選)
    @Override
    public Page<TrialBookingDTO> getAllTrialBookings(Pageable pageable, String bookingStatus, Integer courseId, Integer userId) {
        logger.info("在 TrialBookingServiceImpl 中實作 getAllTrialBookings 方法，參數：頁碼={}, 每頁={}, 狀態={}, 課程ID={}, 使用者ID={}",
                   pageable.getPageNumber(), pageable.getPageSize(), bookingStatus, courseId, userId);

        // 呼叫 DAO 層的方法來處理分頁和篩選
        // DAO 方法 findWithFilters 中的 @Query 應該已經修改以處理 booking_email 和 user IS NULL 的情況

        Page<TrialBooking> trialBookingPage = trialBookingDAO.findWithFilters(pageable, bookingStatus, courseId, userId); // <-- 呼叫 DAO 方法

        // 將查詢結果轉換為 DTO，convertToTrialBookingDTO 會包含 bookingEmail
        List<TrialBookingDTO> trialBookingDTOs = trialBookingPage.getContent().stream()
                                                        .map(this::convertToTrialBookingDTO)
                                                        .collect(Collectors.toList());

        return new PageImpl<>(trialBookingDTOs, pageable, trialBookingPage.getTotalElements());
    }

    // *** 新增: 根據會員名稱查詢體驗預約記錄的實作 ***
    @Override // 標記這個方法是實現 TrialBookingService 介面中的方法
    public List<TrialBookingDTO> searchTrialBookingsByUserName(String userName) {
        logger.info("在 TrialBookingServiceImpl 中實作 searchTrialBookingsByUserName，名稱：{}", userName);

        // *** 呼叫 DAO 層的方法來根據預約姓名查詢 ***
        // 您需要在 TrialBookingDAO 介面中定義一個方法，例如：
        // List<TrialBooking> findByBookingNameContainingIgnoreCase(String bookingName);
        // 並在 DAO 的實作中實現這個查詢邏輯。
        List<TrialBooking> bookings = trialBookingDAO.findByBookingNameContainingIgnoreCase(userName); // <-- 假設 DAO 有此方法

        logger.info("找到符合名稱 '{}' 的 {} 條體驗預約記錄。", userName, bookings.size());

        // 將查詢結果的 Entity 列表轉換為 DTO 列表
        return bookings.stream()
                       .map(this::convertToTrialBookingDTO) // 使用已有的轉換輔助方法
                       .collect(Collectors.toList());
    }
}
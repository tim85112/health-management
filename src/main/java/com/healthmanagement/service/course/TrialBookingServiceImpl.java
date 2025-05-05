package com.healthmanagement.service.course;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.healthmanagement.dao.course.TrialBookingDAO;
import com.healthmanagement.dao.course.CourseDAO;
import com.healthmanagement.dao.member.UserDAO;

import com.healthmanagement.dto.course.TrialBookingRequestDTO;
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
    private UserDAO userDAO;

    @Autowired
    private CourseDAO courseDAO;

    @Autowired
    private EnrollmentService enrollmentService;

    private static final String BOOKED_STATUS = "已預約";
    private static final String CANCELLED_STATUS = "已取消";
    private static final String COMPLETED_STATUS = "已完成";
    private static final String NO_SHOW_STATUS = "未到場";

    private static final List<String> ACTIVE_TRIAL_STATUSES = List.of(BOOKED_STATUS);
    private static final List<String> INACTIVE_TRIAL_STATUSES = List.of(CANCELLED_STATUS, COMPLETED_STATUS, NO_SHOW_STATUS);

    // 設定預約檢查的時限（例如提前 24 小時）
    private static final long BOOKING_CUTOFF_HOURS = 24;

    private TrialBookingDTO convertToTrialBookingDTO(TrialBooking trialBooking) {
        if (trialBooking == null) {
            return null;
        }
        TrialBookingDTO.TrialBookingDTOBuilder builder = TrialBookingDTO.builder()
                    .id(trialBooking.getId())
                    .userId(trialBooking.getUser() != null ? trialBooking.getUser().getId() : null)
                    .courseId(trialBooking.getCourse() != null ? trialBooking.getCourse().getId() : null)
                    .bookingName(trialBooking.getBookingName())
                    .bookingEmail(trialBooking.getBookingEmail())
                    .bookingPhone(trialBooking.getBookingPhone())
                    .bookingStatus(trialBooking.getBookingStatus())
                    .bookedAt(trialBooking.getBookedAt());

        if (trialBooking.getUser() != null) {
            builder.userName(trialBooking.getUser().getName());
        }

        LocalDate bookingDate = trialBooking.getBookingDate();
        LocalTime startTime = null;
        String courseName = null;
        String coachName = null;

        if (trialBooking.getCourse() != null) {
            courseName = trialBooking.getCourse().getName();
            startTime = trialBooking.getCourse().getStartTime();

            if (trialBooking.getCourse().getCoach() != null) {
                coachName = trialBooking.getCourse().getCoach().getName();
            }
        }

        builder.courseName(courseName);
        builder.coachName(coachName);

        if (bookingDate != null && startTime != null) {
            builder.bookingTime(LocalDateTime.of(bookingDate, startTime));
        } else {
            builder.bookingTime(null);
        }

        return builder.build();
    }

    // 預約體驗課程
    @Transactional
    @Override
    public TrialBookingDTO bookTrialCourse(User user, String contactEmail, TrialBookingRequestDTO bookingRequestDTO) {

        if (user != null) {
            logger.info("已認證用戶 (ID: {}) 嘗試預約體驗課程...", user.getId());
        } else {
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
        // 使用 ChronoUnit.HOURS.between 計算小時差
        LocalDateTime bookingDateTime = LocalDateTime.of(bookingRequestDTO.getBookingDate(), course.getStartTime());
        long hoursBetween = ChronoUnit.HOURS.between(LocalDateTime.now(), bookingDateTime);
        if (!bookingDateTime.isAfter(LocalDateTime.now()) || hoursBetween < BOOKING_CUTOFF_HOURS) {
            logger.warn("使用者 {} 預約課程 ID {} 失敗：課程將於 {} 開始，距離不足 {} 小時。",
                         user != null ? "ID=" + user.getId() : "匿名", course.getId(), bookingDateTime, BOOKING_CUTOFF_HOURS);
            throw new IllegalStateException(String.format("預約失敗：預約時間需距離課程開始至少 %d 小時，或預約時間已過。", BOOKING_CUTOFF_HOURS));
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
                user,
                course,
                INACTIVE_TRIAL_STATUSES,
                LocalDate.now()
            );

            if (existingActiveBooking.isPresent()) {
                logger.warn("使用者 ID {} 已存在課程 ID {} 的活躍體驗預約 (ID: {})。",
                            user.getId(), course.getId(), existingActiveBooking.get().getId());
                throw new IllegalStateException("您已預約過此課程的體驗課。");
            }
            logger.info("已認證用戶 (ID: {}) 在課程 ID {} 不存在活躍體驗預約。", user.getId(), course.getId());

        } else {
            logger.info("檢查匿名用戶 (Email: {}, Name: {}, Phone: {}) 是否已預約過課程 ID {} 在 {} 的體驗課...",
                       contactEmail, bookingRequestDTO.getBookingName(), bookingRequestDTO.getBookingPhone(), course.getId(), bookingRequestDTO.getBookingDate());

            existingActiveBooking = trialBookingDAO.findActiveBookingByBookingEmailAndBookingNameAndBookingPhoneAndCourseAndBookingDateAndBookingStatusNotIn(
                contactEmail,
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

        // 建立 TrialBooking 實體，設定 bookingEmail
        TrialBooking trialBooking = TrialBooking.builder()
                .user(user)
                .course(course)
                .bookingName(user != null ? user.getName() : bookingRequestDTO.getBookingName())
                .bookingEmail(contactEmail)
                .bookingPhone(formattedBookingPhone)
                .bookingDate(bookingRequestDTO.getBookingDate())
                .bookingStatus(BOOKED_STATUS)
                .bookedAt(LocalDateTime.now()) // 顯式設定建立時間
                .build();

        TrialBooking savedBooking = trialBookingDAO.save(trialBooking);
        logger.info("體驗預約創建成功 (ID: {})", savedBooking.getId());

        return convertToTrialBookingDTO(savedBooking);
    }

    // 查詢特定使用者的所有體驗預約
    @Override
    @Transactional(readOnly = true)
    public List<TrialBookingDTO> getTrialBookingsByUserId(Integer userId) {
        logger.info("查詢使用者 ID {} 的所有體驗預約記錄...", userId);
        if (userId == null) {
            logger.warn("嘗試使用 null userId 查詢體驗預約記錄。");
            return Collections.emptyList();
        }
        User user = userDAO.findById(userId)
                .orElseThrow(() -> new EntityNotFoundException("找不到使用者 ID: " + userId));

        // TrialBookingDAO 需要一個 findByUser(User user) 或 findByUserId(Integer userId) 方法
        List<TrialBooking> userBookings = trialBookingDAO.findByUser(user);
        logger.info("找到使用者 ID {} 的 {} 條體驗預約記錄。", userId, userBookings.size());
        return userBookings.stream()
                           .map(this::convertToTrialBookingDTO)
                           .collect(Collectors.toList());
    }

    // 查詢特定課程的所有體驗預約
    @Override
    @Transactional(readOnly = true)
    public List<TrialBookingDTO> getTrialBookingsByCourseId(Integer courseId) {
        logger.info("查詢課程 {} 的所有體驗預約...", courseId);
        Course course = courseDAO.findById(courseId)
                .orElseThrow(() -> new EntityNotFoundException("找不到課程 ID: " + courseId));

        // TrialBookingDAO 需要一個 findByCourse(Course course) 或 findByCourseId(Integer courseId) 方法
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
    @Transactional(readOnly = true)
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
        LocalDateTime now = LocalDateTime.now();
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
            booking.setBookingStatus(NO_SHOW_STATUS);
            trialBookingDAO.save(booking);
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
        TrialBooking trialBooking = trialBookingDAO.findById(bookingId)
                .orElseThrow(() -> new EntityNotFoundException("找不到體驗預約 ID: " + bookingId));
        List<String> validManualUpdateStatuses = List.of(BOOKED_STATUS, CANCELLED_STATUS, COMPLETED_STATUS, NO_SHOW_STATUS);
        if (!validManualUpdateStatuses.contains(newStatus)) {
             logger.warn("嘗試將預約 {} 更新為無效狀態: {}", bookingId, newStatus);
            throw new IllegalArgumentException("無效的體驗預約目標狀態: " + newStatus);
        }
        String currentStatus = trialBooking.getBookingStatus();
        if (INACTIVE_TRIAL_STATUSES.contains(currentStatus)) {
            logger.warn("無法從目前狀態 '{}' 更改預約 {} 的狀態。", currentStatus, bookingId);
            throw new IllegalStateException(String.format("無法從目前狀態 '%s' 更改預約狀態。", currentStatus));
        }
        // TODO: 可選：更嚴格的狀態轉換規則驗證 (例如，只能從 BOOKED 轉為 COMPLETED/NO_SHOW/CANCELLED)

        trialBooking.setBookingStatus(newStatus);
        TrialBooking updatedBooking = trialBookingDAO.save(trialBooking);
        logger.info("體驗預約 ID {} 狀態更新為 {} 成功。", bookingId, newStatus);
        return convertToTrialBookingDTO(updatedBooking);
    }

    // 電話號碼格式化輔助方法
    private String formatPhoneNumber(String rawPhoneNumber) {
        if (rawPhoneNumber == null) {
            return null;
        }
        String digitsOnly = rawPhoneNumber.replaceAll("\\D", "");
        if (digitsOnly.length() != 10 || !digitsOnly.startsWith("09")) {
            logger.warn("Received phone number in unexpected format or invalid: {}. Cleaned digits: {}", rawPhoneNumber, digitsOnly);
            return digitsOnly;
        }
        return digitsOnly.substring(0, 4) + "-" + digitsOnly.substring(4);
    }

	// 獲取所有體驗預約紀錄 (支援分頁和篩選)
	@Override
	@Transactional(readOnly = true)
	public Page<TrialBookingDTO> getAllTrialBookings(Pageable pageable, String bookingStatus, Integer courseId, Integer userId) {
		logger.info("在 TrialBookingServiceImpl 中實作 getAllTrialBookings 方法，參數：頁碼={}, 每頁={}, 狀態={}, 課程ID={}, 使用者ID={}",
				pageable.getPageNumber(), pageable.getPageSize(), bookingStatus, courseId, userId);

		// 呼叫 DAO 層的方法來處理分頁和篩選
		// DAO 方法 findWithFilters 中的 @Query 應該已經修改以處理 booking_email 和 user IS NULL 的情況
		// 強烈建議在 TrialBookingDAO 的 findWithFilters 方法中配置對 course 和 user 的立即載入
		Page<TrialBooking> trialBookingPage = trialBookingDAO.findWithFilters(pageable, bookingStatus, courseId, userId);

        // 將查詢結果轉換為 DTO，convertToTrialBookingDTO 會包含 bookingEmail
        List<TrialBookingDTO> trialBookingDTOs = trialBookingPage.getContent().stream()
                                                        .map(this::convertToTrialBookingDTO)
                                                        .collect(Collectors.toList());

        return new PageImpl<>(trialBookingDTOs, pageable, trialBookingPage.getTotalElements());
    }

	// 根據會員名稱查詢體驗預約記錄的實作
	@Override
	@Transactional(readOnly = true)
	public List<TrialBookingDTO> searchTrialBookingsByUserName(String userName) {
		logger.info("在 TrialBookingServiceImpl 中實作 searchTrialBookingsByUserName，名稱：{}", userName);

		// 呼叫 DAO 層的方法來根據預約姓名查詢
		// 您需要在 TrialBookingDAO 介面中定義一個方法，例如：
		// List<TrialBooking> findByBookingNameContainingIgnoreCase(String bookingName);
		// 強烈建議在這個 DAO 方法中配置對 course 和 user 的立即載入
		List<TrialBooking> bookings = trialBookingDAO.findByBookingNameContainingIgnoreCase(userName);

		logger.info("找到符合名稱 '{}' 的 {} 條體驗預約記錄。", userName, bookings.size());

		return bookings.stream()
						.map(this::convertToTrialBookingDTO)
						.collect(Collectors.toList());
    }
}
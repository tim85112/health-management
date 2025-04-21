package com.healthmanagement.controller.course;

import com.healthmanagement.dto.course.TrialBookingRequestDTO;
import com.healthmanagement.dto.course.TrialBookingDTO;
import com.healthmanagement.dto.course.BookingStatusUpdateDTO;
import com.healthmanagement.dto.course.ErrorResponse;
import com.healthmanagement.model.member.User;
import com.healthmanagement.security.UserSecurity;
import com.healthmanagement.service.course.TrialBookingService;
import com.healthmanagement.service.member.UserService;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.persistence.EntityNotFoundException;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/trial-bookings")
@CrossOrigin(origins = "*") // 允許跨域請求
@Tag(name = "體驗課程預約管理", description = "體驗課程預約管理API")
public class TrialBookingController {

	private static final Logger logger = LoggerFactory.getLogger(TrialBookingController.class);
	
    private final TrialBookingService trialBookingService;
    private final UserService userService;
    private final UserSecurity userSecurity;

    @Autowired
    public TrialBookingController(TrialBookingService trialBookingService, UserService userService, UserSecurity userSecurity) {
        this.trialBookingService = trialBookingService;
        this.userService = userService;
        this.userSecurity = userSecurity;
    }

    @Operation(summary = "預約體驗課程 (需user或guest權限)")
    @PreAuthorize("hasAnyAuthority('user', 'guest')")
    @PostMapping
    public ResponseEntity<?> bookTrialCourse(@AuthenticationPrincipal UserDetails userDetails,
                                             @Valid @RequestBody TrialBookingRequestDTO bookingRequestDTO) {
        try {
            Integer userId = null;
            if (userDetails != null) {
                String email = userDetails.getUsername();
                User user = userService.findByEmail(email)
                        .orElseThrow(() -> new IllegalStateException("無法找到對應的使用者或使用者資訊不完整"));
                userId = user.getId();
                if (bookingRequestDTO.getBookingName() == null || bookingRequestDTO.getBookingName().trim().isEmpty()) {
                    bookingRequestDTO.setBookingName(user.getName()); // <-- 只在這裡設定 user.getName()
                    logger.info("已認證用戶 (ID: {}) 預約，DTO bookingName 為空，使用其姓名作為 bookingName: {}", userId, user.getName());
               } else {
                    // Request Body 提供了 bookingName，直接使用它
                    logger.info("已認證用戶 (ID: {}) 預約，使用DTO提供的 bookingName: {}", userId, bookingRequestDTO.getBookingName());
               }

               // **重要的檢查：確保手機號碼非空**
               if (bookingRequestDTO.getBookingPhone() == null || bookingRequestDTO.getBookingPhone().trim().isEmpty()) {
                   logger.warn("已認證用戶 (ID: {}) 預約，DTO bookingPhone 為空。預約需要手機號碼。", userId);
                   throw new IllegalStateException("預約需要提供手機號碼。"); // 要求已認證用戶也必須提供手機號碼
               }


           } else {
               // **更動：匿名預約時，確保姓名和手機號碼都非空**
               logger.info("匿名預約請求，userDetails 為 null。");
               if (bookingRequestDTO.getBookingName() == null || bookingRequestDTO.getBookingName().trim().isEmpty() ||
                   bookingRequestDTO.getBookingPhone() == null || bookingRequestDTO.getBookingPhone().trim().isEmpty()) {
                   logger.warn("匿名用戶預約請求，姓名或手機號碼為空。預約需要姓名和手機號碼。");
                   throw new IllegalStateException("匿名預約需要提供姓名和手機號碼。");
               }
           }
            TrialBookingDTO bookedTrialBooking = trialBookingService.bookTrialCourse(userId, bookingRequestDTO);
            return new ResponseEntity<>(bookedTrialBooking, HttpStatus.CREATED);
        } catch (EntityNotFoundException e) {
            logger.error("Booking failed: Entity not found.", e);
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(new ErrorResponse(e.getMessage()));
        } catch (AccessDeniedException e) {
            logger.warn("Booking failed: Access denied.", e);
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(new ErrorResponse(e.getMessage()));
        } catch (IllegalStateException e) {
            logger.warn("Booking failed: Illegal state or business rule violation.", e);
            return ResponseEntity.badRequest().body(new ErrorResponse(e.getMessage()));
        } catch (Exception e) {
            logger.error("Booking failed: Internal server error.", e);
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(new ErrorResponse("預約失敗，服務內部錯誤。請稍後再試或聯繫管理員。"));
        }
    }

    @Operation(summary = "查詢特定使用者的所有體驗預約 (需本人或 admin/coach)")
    @PreAuthorize("hasAnyAuthority('admin', 'coach') or @userSecurity.isCurrentUser(#userId)")
    @GetMapping("/users/{userId}")
    public ResponseEntity<?> getTrialBookingsByUserId(@PathVariable Integer userId) {
        try {
            List<TrialBookingDTO> userBookings = trialBookingService.getTrialBookingsByUserId(userId);
            return ResponseEntity.ok(userBookings);
        } catch (EntityNotFoundException e) {
             logger.error("查詢使用者 {} 的預約失敗: 使用者未找到", userId, e);
             return ResponseEntity.status(HttpStatus.NOT_FOUND).body(new ErrorResponse(e.getMessage()));
        } catch (AccessDeniedException e) {
             logger.warn("查詢使用者 {} 的預約失敗: 權限不足", userId, e);
             return ResponseEntity.status(HttpStatus.FORBIDDEN).body(new ErrorResponse(e.getMessage()));
        } catch (Exception e) {
             logger.error("查詢使用者 {} 的預約失敗，服務內部錯誤", userId, e);
             return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(new ErrorResponse("查詢預約失敗，服務內部錯誤。"));
        }
    }

    @Operation(summary = "查詢特定課程的所有體驗預約 (需admin/coach)")
    @PreAuthorize("hasAnyAuthority('admin', 'coach')")
    @GetMapping("/courses/{courseId}")
    public ResponseEntity<?> getTrialBookingsByCourseId(@PathVariable Integer courseId) {
        try {
            List<TrialBookingDTO> courseBookings = trialBookingService.getTrialBookingsByCourseId(courseId);
            return ResponseEntity.ok(courseBookings);
        } catch (EntityNotFoundException e) {
             logger.error("查詢課程 {} 的預約失敗: 課程未找到", courseId, e);
             return ResponseEntity.status(HttpStatus.NOT_FOUND).body(new ErrorResponse(e.getMessage()));
        } catch (AccessDeniedException e) {
             logger.warn("查詢課程 {} 的預約失敗: 權限不足", courseId, e);
             return ResponseEntity.status(HttpStatus.FORBIDDEN).body(new ErrorResponse(e.getMessage()));
        } catch (Exception e) {
             logger.error("查詢課程 {} 的預約失敗，服務內部錯誤", courseId, e);
             return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(new ErrorResponse("查詢預約失敗，服務內部錯誤。"));
        }
    }

    @Operation(summary = "查詢單個體驗預約詳情 (需預約者本人或 admin/coach)")
    @PreAuthorize("hasAnyAuthority('admin', 'coach') or @userSecurity.isCurrentUserByTrialBookingId(#bookingId)")
    @GetMapping("/{bookingId}")
    public ResponseEntity<?> getTrialBookingDetails(@PathVariable Integer bookingId) {
        try {
            TrialBookingDTO bookingDetails = trialBookingService.getTrialBookingDetails(bookingId);
            return ResponseEntity.ok(bookingDetails);
        } catch (EntityNotFoundException e) {
            logger.error("查詢預約 {} 詳情失敗: 預約未找到", bookingId, e);
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(new ErrorResponse(e.getMessage()));
        } catch (AccessDeniedException e) {
            logger.warn("查詢預約 {} 詳情失敗: 權限不足", bookingId, e);
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(new ErrorResponse(e.getMessage()));
        } catch (Exception e) {
            logger.error("查詢預約 {} 詳情失敗，服務內部錯誤", bookingId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(new ErrorResponse("查詢預約詳情失敗，服務內部錯誤。"));
        }
    }
    
    @Operation(summary = "取消體驗預約 (需預約者本人或 admin/coach)") // guest不能取消
    @PreAuthorize("hasAnyAuthority('admin', 'coach') or @userSecurity.isCurrentUserByTrialBookingId(#bookingId)")
    @DeleteMapping("/{bookingId}")
    public ResponseEntity<ErrorResponse> cancelTrialBooking(@PathVariable Integer bookingId) {
        try {
            trialBookingService.cancelTrialBooking(bookingId);
            return ResponseEntity.ok().body(new ErrorResponse("體驗預約取消成功。"));
        } catch (EntityNotFoundException e) {
            logger.error("取消預約 {} 失敗: 預約未找到", bookingId, e);
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(new ErrorResponse(e.getMessage()));
        } catch (AccessDeniedException e) {
            logger.warn("取消預約 {} 失敗: 權限不足", bookingId, e);
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(new ErrorResponse(e.getMessage()));
        } catch (IllegalStateException e) {
            logger.error("取消預約 {} 失敗: 狀態異常或業務規則違規", bookingId, e);
            return ResponseEntity.badRequest().body(new ErrorResponse(e.getMessage()));
        } catch (Exception e) {
            logger.error("取消預約 {} 失敗，服務內部錯誤", bookingId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(new ErrorResponse("體驗預約取消失敗: " + e.getMessage()));
        }
    }

    @Operation(summary = "手動更新體驗預約狀態 (需 admin/coach 權限)")
    @PreAuthorize("hasAnyAuthority('admin', 'coach')")
    @PutMapping("/{bookingId}/status")
    public ResponseEntity<?> updateTrialBookingStatus(@PathVariable Integer bookingId,
            @RequestBody BookingStatusUpdateDTO statusUpdateDTO) {
		try {
			TrialBookingDTO updatedBooking = trialBookingService.updateBookingStatus(bookingId, statusUpdateDTO.getNewStatus());
			return ResponseEntity.ok(updatedBooking);
		} catch (EntityNotFoundException e) {
			logger.error("更新預約 {} 狀態失敗: 預約未找到", bookingId, e);
		// **更動：返回標準錯誤回應**
			return ResponseEntity.status(HttpStatus.NOT_FOUND).body(new ErrorResponse(e.getMessage()));
		} catch (IllegalStateException e) {
			logger.error("更新預約 {} 狀態失敗: 狀態異常或業務規則違規", bookingId, e);
			return ResponseEntity.badRequest().body(new ErrorResponse(e.getMessage()));
		} catch (AccessDeniedException e) {
			logger.warn("更新預約 {} 狀態失敗: 權限不足", bookingId, e);
			return ResponseEntity.status(HttpStatus.FORBIDDEN).body(new ErrorResponse(e.getMessage()));
		} catch (Exception e) {
			logger.error("更新預約 {} 狀態失敗，服務內部錯誤", bookingId, e);
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(new ErrorResponse("更新預約狀態失敗，服務內部錯誤。"));
		}
	}
}
package com.healthmanagement.controller.course;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;

import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import com.healthmanagement.dto.course.TrialBookingRequestDTO;
import com.healthmanagement.model.member.User;
import com.healthmanagement.dto.course.TrialBookingDTO;
import com.healthmanagement.dto.course.BookingStatusUpdateDTO;
import com.healthmanagement.service.course.TrialBookingService;
import com.healthmanagement.service.member.UserService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

import jakarta.persistence.EntityNotFoundException;

import java.util.List;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@RestController
@RequestMapping("/api/trial-bookings")
@CrossOrigin(origins = "*")
@Tag(name = "體驗預約管理", description = "體驗預約管理API")
public class TrialBookingController {

    private static final Logger logger = LoggerFactory.getLogger(TrialBookingController.class);

    private final TrialBookingService trialBookingService;

    private final UserService userService;

    @Autowired
    public TrialBookingController(TrialBookingService trialBookingService, UserService userService) {
        this.trialBookingService = trialBookingService;
        this.userService = userService;
    }

    // 預約體驗課程
    @Operation(summary = "預約體驗課程 (需user/guest)")
    @PreAuthorize("hasAnyAuthority('user', 'guest')")
    @PostMapping("/book")
    public ResponseEntity<TrialBookingDTO> bookTrialCourse(@RequestBody TrialBookingRequestDTO bookingRequestDTO) {
        logger.info("收到預約體驗課程請求：課程 ID {}，日期 {}，姓名 {}，電話 {}。",
                    bookingRequestDTO.getCourseId(), bookingRequestDTO.getBookingDate(),
                    bookingRequestDTO.getBookingName(), bookingRequestDTO.getBookingPhone());

        User user = null;
        String contactEmail;

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication != null && authentication.isAuthenticated() && !authentication.getPrincipal().equals("anonymousUser")) {
             Object principal = authentication.getPrincipal();

             if (principal instanceof UserDetails) {
                  UserDetails userDetails = (UserDetails) principal;
                  String usernameFromPrincipal = userDetails.getUsername();
                  logger.info(" Principal 是 UserDetails 類型。獲取到的用戶名: {}", usernameFromPrincipal);

                  try {
                      Optional<User> userOptional = userService.findByEmail(usernameFromPrincipal);
                      if (userOptional.isPresent()) {
                          user = userOptional.get();
                          contactEmail = user.getEmail();
                          logger.info("根據用戶名 '{}' 找到用戶，使用其註冊郵箱: {}", usernameFromPrincipal, contactEmail);
                      } else {
                           logger.warn(" Principal 是 UserDetails 類型，但根據用戶名 '{}' 在資料庫中找不到對應的用戶。", usernameFromPrincipal);
                           contactEmail = bookingRequestDTO.getBookingEmail();
                           logger.warn(" 作為匿名處理，使用 DTO 中的郵箱: {}", contactEmail);
                      }
                  } catch (Exception e) {
                      logger.error(" 處理 UserDetails Principal 或根據用戶名查找用戶時發生錯誤。", e);
                      contactEmail = bookingRequestDTO.getBookingEmail();
                      logger.warn(" 處理Principal時發生錯誤，作為匿名處理，使用 DTO 中的郵箱: {}", contactEmail);
                  }
             } else {
                  logger.warn(" Principal 是未知類型 {}，無法自動獲取使用者資訊。", principal != null ? principal.getClass().getName() : "null");
                  contactEmail = bookingRequestDTO.getBookingEmail();
                  logger.warn(" 作為匿名處理，使用 DTO 中的郵箱: {}", contactEmail);
             }

        } else {
             logger.info(" 使用者未認證或為匿名用戶。");
             user = null;
             contactEmail = bookingRequestDTO.getBookingEmail();
             logger.info(" 從 DTO 中獲取郵箱供匿名預約使用: {}", contactEmail);

             if (contactEmail == null || contactEmail.trim().isEmpty()) {
                 logger.warn(" 匿名預約請求缺少郵箱。");
                 return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(null);
             }
        }

        logger.info(" 準備將預約資訊傳遞給 Service：使用者 {}，聯絡郵箱 {}，課程 ID {}，日期 {}。",
                     user != null ? "ID=" + user.getId() : "匿名", contactEmail,
                     bookingRequestDTO.getCourseId(), bookingRequestDTO.getBookingDate());


        try {
            TrialBookingDTO bookedBooking = trialBookingService.bookTrialCourse(
                user,
                contactEmail,
                bookingRequestDTO
            );

            logger.info("體驗預約成功，預約 ID：{}。", bookedBooking.getId());
            return ResponseEntity.status(HttpStatus.CREATED).body(bookedBooking);

        } catch (EntityNotFoundException e) {
            logger.warn(" 預約失敗：找不到相關課程或其他必要資源。", e);
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(null);
        } catch (IllegalStateException e) {
            logger.warn(" 預約失敗：業務邏輯錯誤。訊息: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(null);
        } catch (Exception e) {
            logger.error(" 預約體驗課程時發生內部錯誤。", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null);
        }
    }

    // 取消體驗預約
    @Operation(summary = "取消體驗預約 (需本人/admin)")
    @PreAuthorize("hasAuthority('admin') or @userSecurity.isTrialBookingOwner(#bookingId)")
    @DeleteMapping("/{bookingId}")
    public ResponseEntity<Void> cancelTrialBooking(@PathVariable Integer bookingId) {
         logger.info("收到取消體驗預約請求，預約 ID：{}。", bookingId);
        try {
            trialBookingService.cancelTrialBooking(bookingId);
            logger.info("體驗預約 ID {} 取消成功。", bookingId);
            return ResponseEntity.noContent().build();
        } catch (EntityNotFoundException e) {
             logger.warn("取消失敗：找不到體驗預約 ID: {}。", bookingId, e);
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        } catch (IllegalStateException e) {
             logger.warn("取消失敗：預約狀態不允許取消，預約 ID: {}。", bookingId, e);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(null);
        } catch (Exception e) {
             logger.error("取消體驗預約 ID {} 時發生內部錯誤。", bookingId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null);
        }
    }

    // 查詢特定使用者的所有體驗預約 (需本人或 admin/coach)
    @Operation(summary = "查詢特定使用者的所有體驗預約 (需本人或 admin/coach)")
    @PreAuthorize("hasAnyAuthority('admin', 'coach') or @userSecurity.isCurrentUser(#userId)")
    @GetMapping("/user/{userId}")
    public ResponseEntity<List<TrialBookingDTO>> getTrialBookingsByUserId(@PathVariable Integer userId) {
         logger.info("收到查詢使用者 ID {} 的所有體驗預約請求。", userId);
        try {
            List<TrialBookingDTO> userBookings = trialBookingService.getTrialBookingsByUserId(userId);
            logger.info("返回使用者 ID {} 的 {} 條體驗預約記錄。", userId, userBookings.size());
            return ResponseEntity.ok(userBookings);
        } catch (EntityNotFoundException e) {
            logger.warn("查詢失敗：找不到使用者 ID: {}。", userId, e);
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(null);
        } catch (Exception e) {
            logger.error("查詢使用者 ID {} 的體驗預約時發生內部錯誤。", userId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null);
        }
    }

    // 查詢特定課程的所有體驗預約 (需admin/coach)
    @Operation(summary = "查詢特定課程的所有體驗預約 (需admin/coach)")
    @PreAuthorize("hasAnyAuthority('admin', 'coach')")
    @GetMapping("/course/{courseId}")
    public ResponseEntity<List<TrialBookingDTO>> getTrialBookingsByCourseId(@PathVariable Integer courseId) {
         logger.info("收到查詢課程 ID {} 的所有體驗預約請求。", courseId);
        try {
            List<TrialBookingDTO> courseBookings = trialBookingService.getTrialBookingsByCourseId(courseId);
            logger.info("返回課程 ID {} 的 {} 條體驗預約記錄。", courseId, courseBookings.size());
            return ResponseEntity.ok(courseBookings);
        } catch (EntityNotFoundException e) {
             logger.warn("查詢失敗：找不到課程 ID: {}。", courseId, e);
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(null);
        } catch (Exception e) {
             logger.error("查詢課程 ID {} 的體驗預約時發生內部錯誤。", courseId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null);
        }
    }

    // 查詢單個體驗預約詳細信息 (需本人或 admin)
    @Operation(summary = "查詢單個體驗預約詳細信息 (需本人或 admin)")
    @PreAuthorize("hasAuthority('admin') or @userSecurity.isTrialBookingOwner(#bookingId, authentication.principal)")
    @GetMapping("/{bookingId}")
    public ResponseEntity<TrialBookingDTO> getTrialBookingDetails(@PathVariable Integer bookingId) {
         logger.info("收到查詢體驗預約 ID {} 詳細資訊請求。", bookingId);
        try {
            TrialBookingDTO trialBooking = trialBookingService.getTrialBookingDetails(bookingId);
            logger.info("返回體驗預約 ID {} 詳細資訊。", bookingId);
            return ResponseEntity.ok(trialBooking);
        } catch (EntityNotFoundException e) {
             logger.warn("查詢失敗：找不到體驗預約 ID: {}。", bookingId, e);
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(null);
        } catch (Exception e) {
             logger.error("查詢體驗預約 ID {} 時發生內部錯誤。", bookingId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null);
        }
    }

    // 手動更新體驗預約狀態 (通常只有管理員或教練權限)
    @Operation(summary = "手動更新體驗預約狀態 (需admin/coach)")
    @PreAuthorize("hasAnyAuthority('admin', 'coach')")
    @PutMapping("/{bookingId}/status")
    public ResponseEntity<TrialBookingDTO> updateBookingStatus(@PathVariable Integer bookingId, @RequestBody BookingStatusUpdateDTO updateDTO) {
        String newStatus = updateDTO.getStatus();
         logger.info("收到更新體驗預約 ID {} 狀態請求，新狀態：{}。", bookingId, newStatus);
        try {
            TrialBookingDTO updatedBooking = trialBookingService.updateBookingStatus(bookingId, newStatus);
            logger.info("體驗預約 ID {} 狀態更新成功，新狀態：{}。", updatedBooking.getId(), updatedBooking.getBookingStatus());
            return ResponseEntity.ok(updatedBooking);
        } catch (EntityNotFoundException e) {
             logger.warn("更新狀態失敗：找不到體驗預約 ID: {}。", bookingId, e);
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(null);
        } catch (IllegalArgumentException e) {
             logger.warn("更新狀態失敗：狀態無效，預約 ID: {}，新狀態：{}。", bookingId, newStatus, e);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(null);
        } catch (IllegalStateException e) {
             logger.warn("更新狀態失敗：預約狀態不允許轉換，預約 ID: {}。", bookingId, e);
            return ResponseEntity.status(HttpStatus.CONFLICT).body(null);
        } catch (Exception e) {
             logger.error("更新體驗預約 ID {} 狀態時發生內部錯誤。", bookingId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null);
        }
    }

    // 獲取所有體驗預約紀錄 (支援分頁和篩選)
    @Operation(summary = "獲取所有體驗預約紀錄 (支援分頁、篩選) (需admin/coach)")
    @PreAuthorize("hasAnyAuthority('admin', 'coach')")
    @GetMapping
    public ResponseEntity<Page<TrialBookingDTO>> getAllTrialBookings(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(name = "status", required = false) String bookingStatus,
            @RequestParam(required = false) Integer courseId,
            @RequestParam(required = false) Integer userId
    ) {
        logger.info("收到獲取體驗預約列表請求 - 頁碼: {}, 每頁: {}, 狀態過濾: {}, 課程ID過濾: {}, 使用者ID過濾: {}",
                page, size, bookingStatus, courseId, userId);
        try {
            Pageable pageable = PageRequest.of(page, size);
            Page<TrialBookingDTO> bookingPage = trialBookingService.getAllTrialBookings(
                    pageable, bookingStatus, courseId, userId
            );
            logger.info("返回符合篩選條件的 {} 個體驗預約記錄 (共 {} 頁)。",
                    bookingPage.getTotalElements(), bookingPage.getTotalPages());
            return ResponseEntity.ok(bookingPage);
        } catch (Exception e) {
            logger.error("獲取體驗預約列表時發生內部錯誤。", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null);
        }
    }

    // 依會員名稱查詢
    @Operation(summary = "依會員名稱查詢")
    @PreAuthorize("hasAnyAuthority('admin', 'coach')")
    @GetMapping("/search/by-user-name")
    public ResponseEntity<List<TrialBookingDTO>> searchTrialBookingsByUserName(@RequestParam String name) {
         logger.info("收到依會員名稱查詢體驗預約請求，名稱：{}。", name);

         try {
            List<TrialBookingDTO> bookings = trialBookingService.searchTrialBookingsByUserName(name);
            logger.info("返回符合名稱 '{}' 的 {} 條體驗預約記錄。", name, bookings.size());
            return ResponseEntity.ok(bookings);
         } catch (Exception e) {
            logger.error("依會員名稱查詢體驗預約時發生內部錯誤。", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null);
         }
    }
}
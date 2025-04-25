package com.healthmanagement.controller.course; // 請根據你的實際包結構調整

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize; // 引入 Spring Security 的 PreAuthorize
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import com.healthmanagement.dto.course.TrialBookingRequestDTO;
import com.healthmanagement.model.member.User;
import com.healthmanagement.dto.course.TrialBookingDTO;
import com.healthmanagement.dto.course.BookingStatusUpdateDTO; // 引入 BookingStatusUpdateDTO
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
@RequestMapping("/api/trial-bookings") // 設定基礎路徑
@CrossOrigin(origins = "*") // 允許跨域請求
@Tag(name = "體驗預約管理", description = "體驗預約管理API") // Swagger Tag
public class TrialBookingController {

    private static final Logger logger = LoggerFactory.getLogger(TrialBookingController.class);

    private final TrialBookingService trialBookingService;
    
    @Autowired
    private UserService userService;

    @Autowired
    public TrialBookingController(TrialBookingService trialBookingService) {
        this.trialBookingService = trialBookingService;
    }

    // 預約體驗課程
    @Operation(summary = "預約體驗課程 (需user/guest)")
    @PreAuthorize("hasAnyAuthority('user', 'guest')") // Pre-auth 處理基礎訪問控制
    @PostMapping("/book")
    public ResponseEntity<TrialBookingDTO> bookTrialCourse(@RequestBody TrialBookingRequestDTO bookingRequestDTO) {
        logger.info("收到預約體驗課程請求：課程 ID {}，日期 {}。",
                    bookingRequestDTO.getCourseId(), bookingRequestDTO.getBookingDate());

        // *** MODIFICATION START: 修正從 Principal 中獲取 userId 的邏輯，處理標準 UserDetails (以郵箱為用戶名) ***
        Integer userId = null; // 預設為 null，表示是訪客或無法獲取 ID
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        // 只有在用戶已認證且 Principal 不是匿名用戶時才嘗試獲取 ID
        if (authentication != null && authentication.isAuthenticated() && !authentication.getPrincipal().equals("anonymousUser")) {
             Object principal = authentication.getPrincipal();

             // *** 根據日誌，Principal 是 org.springframework.security.core.userdetails.User (UserDetails 實作) ***
             if (principal instanceof UserDetails) {
                  // 情境 1: Principal 是一個標準的 UserDetails 實作 (例如 org.springframework.security.core.userdetails.User)
                   try {
                        UserDetails userDetails = (UserDetails) principal;
                        // 從 UserDetails 中獲取用戶名，根據日誌，這就是郵箱
                        String usernameFromPrincipal = userDetails.getUsername();
                        logger.info("Principal 是 UserDetails 類型。獲取到的用戶名 (郵箱): {}", usernameFromPrincipal);

                        // *** 通過 UserService 根據郵箱查找 User 實體，以獲取其資料庫 ID ***
                        // 需要 UserService 依賴並有 findByEmail 方法
                        Optional<User> userOptional = userService.findByEmail(usernameFromPrincipal);
                        if (userOptional.isPresent()) {
                            userId = userOptional.get().getId();
                             logger.info("根據郵箱 '{}' 找到用戶，獲取到的 userId: {}", usernameFromPrincipal, userId);
                        } else {
                            logger.warn("Principal 是 UserDetails 類型，但根據用戶名 (郵箱) '{}' 在資料庫中找不到對應的用戶。", usernameFromPrincipal);
                            // userId 依然是 null
                        }

                   } catch (Exception e) {
                       // 捕獲查找過程中可能發生的異常
                        logger.error("處理 UserDetails Principal 或根據郵箱查找用戶時發生錯誤。", e);
                         // userId 依然是 null
                   }
             }
             // 如果你還使用其他 Principal 類型（例如 Integer ID 或 String ID），請保留或調整相應的 else if 分支
             // 但你目前說不使用 OAuth2 了，Principal 類型預計主要是 UserDetails 或 Integer
             /*
             else if (principal instanceof Integer) {
                  // 情境 2: Principal 直接就是 Integer 類型的用戶 ID
                  userId = (Integer) principal;
                  logger.info("Principal 是 Integer。獲取到的 userId: {}", userId);
             } else if (principal instanceof String) {
                  // 情境 3: Principal 是 String (例如用戶名或郵箱字串)
                  // ... 處理 String Principal 的邏輯 ...
             }
             */
             else {
                  // 情境 4: 其他未知 Principal 類型
                  logger.warn("Principal 是未知類型 {}。無法自動提取使用者 ID。", principal != null ? principal.getClass().getName() : "null");
                  // userId 依然是 null
             }
         } // else 區塊 (未認證用戶) - userId 保持 null


        // 在這裡，userId 變數應該是：
        // - 已登入用戶的 Integer ID (如果 Principal 成功被處理且用戶被找到)
        // - null (如果是匿名用戶，或 Principal 無法被處理，或根據 Principal 找不到用戶)

        logger.info("決定傳遞給 TrialBookingService 的 userId 為: {}", userId != null ? userId : "null (匿名用戶)");
        // *** MODIFICATION END ***


        // ... 後面的程式碼，包括調用 Service 和錯誤處理，保持不變 ...
        logger.info("預約體驗課程：使用者 ID {}，課程 ID {}。", userId != null ? userId : "訪客", bookingRequestDTO.getCourseId());

        try {
            // 將確定好的 userId 傳遞給 Service 方法
            TrialBookingDTO bookedBooking = trialBookingService.bookTrialCourse(userId, bookingRequestDTO);
            logger.info("體驗預約成功，預約 ID：{}。", bookedBooking.getId());
            return ResponseEntity.status(HttpStatus.CREATED).body(bookedBooking);
        } catch (EntityNotFoundException e) {
            // 這可能發生在根據 userId 查找用戶失敗，或者在 Service 中查找訪客帳號失敗
            logger.warn("預約失敗：找不到相關資源或使用者/訪客帳號。", e);
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(null);
        } catch (IllegalStateException e) {
            logger.warn("預約失敗：業務邏輯錯誤 (例如課程不提供體驗, 已報名常規課, 時間不符, 已預約, 人數已滿)。訊息: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(null); // 或 HttpStatus.CONFLICT (409)
        } catch (Exception e) {
            logger.error("預約體驗課程時發生內部錯誤。", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null);
        }
    }

    // 取消體驗預約 (可能需要使用者本人或管理員權限)
    @Operation(summary = "取消體驗預約 (需本人/admin)")
    @PreAuthorize("hasAuthority('admin') or @userSecurity.isTrialBookingOwner(#bookingId, authentication.principal))")
    @DeleteMapping("/{bookingId}")
    public ResponseEntity<Void> cancelTrialBooking(@PathVariable Integer bookingId) {
        logger.info("收到取消體驗預約請求，預約 ID：{}。", bookingId);
        // TODO: 添加權限檢查，確保使用者有權限取消這個預約
        // 例如：獲取預約的 userId，與當前登入的 userId 比較

        try {
            trialBookingService.cancelTrialBooking(bookingId);
            logger.info("體驗預約 ID {} 取消成功。", bookingId);
            return ResponseEntity.noContent().build();
        } catch (EntityNotFoundException e) {
             logger.warn("取消失敗：找不到體驗預約 ID: {}。", bookingId, e);
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        } catch (IllegalStateException e) {
             logger.warn("取消失敗：預約狀態不允許取消，預約 ID: {}。", bookingId, e);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(null); // 或 HttpStatus.CONFLICT (409)
        } catch (Exception e) {
             logger.error("取消體驗預約 ID {} 時發生內部錯誤。", bookingId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null);
        }
    }

    // 查詢特定使用者的所有體驗預約 (需要使用者本人或管理員權限)
    @Operation(summary = "查詢特定使用者的所有體驗預約 (需本人或 admin/coach)")
    @PreAuthorize("hasAnyAuthority('admin', 'coach') or #userId == authentication.principal)")
    @GetMapping("/user/{userId}")
    public ResponseEntity<List<TrialBookingDTO>> getTrialBookingsByUserId(@PathVariable Integer userId) {
        logger.info("收到查詢使用者 ID {} 的所有體驗預約請求。", userId);
        // TODO: 添加權限檢查，確保使用者有權限查詢這個使用者的預約

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

    // 查詢特定課程的所有體驗預約 (可能需要管理員或教練權限)
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

    // 查詢單個體驗預約詳細信息 (可能需要使用者本人或管理員權限)
    @Operation(summary = "查詢單個體驗預約詳細信息 (需本人或 admin)")
    @PreAuthorize("hasAuthority('admin') or @userSecurity.isTrialBookingOwner(#bookingId, authentication.principal))")
    @GetMapping("/{bookingId}")
    public ResponseEntity<TrialBookingDTO> getTrialBookingDetails(@PathVariable Integer bookingId) {
        logger.info("收到查詢體驗預約 ID {} 詳細資訊請求。", bookingId);
        // TODO: 添加權限檢查，確保使用者有權限查看這個預約

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
    @PreAuthorize("hasAnyAuthority('admin', 'coach')") // 範例：只有管理員或教練能更新狀態
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
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(null); // 無效狀態
        } catch (IllegalStateException e) {
             logger.warn("更新狀態失敗：預約狀態不允許轉換，預約 ID: {}。", bookingId, e);
            return ResponseEntity.status(HttpStatus.CONFLICT).body(null); // 狀態轉換無效
        } catch (Exception e) {
             logger.error("更新體驗預約 ID {} 狀態時發生內部錯誤。", bookingId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null);
        }
    }
}
package com.healthmanagement.controller.course; // 請根據你的實際包結構調整

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize; // 引入 Spring Security 的 PreAuthorize
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails; // 可能會用到 UserDetails
import org.springframework.web.bind.annotation.*;

import com.healthmanagement.dto.course.TrialBookingRequestDTO;
import com.healthmanagement.model.member.User; // 確保引入 User 實體
import com.healthmanagement.dto.course.TrialBookingDTO;
import com.healthmanagement.dto.course.BookingStatusUpdateDTO; // 引入 BookingStatusUpdateDTO
import com.healthmanagement.service.course.TrialBookingService;
import com.healthmanagement.service.member.UserService; // 確保引入 UserService

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

import jakarta.persistence.EntityNotFoundException;

import java.util.List;
import java.util.Optional; // 引入 Optional

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@RestController
@RequestMapping("/api/trial-bookings") // 設定基礎路徑
@CrossOrigin(origins = "*") // 允許跨域請求
@Tag(name = "體驗預約管理", description = "體驗預約管理API") // Swagger Tag
public class TrialBookingController {

    private static final Logger logger = LoggerFactory.getLogger(TrialBookingController.class);

    private final TrialBookingService trialBookingService;
    
    @Autowired // 注入 UserService
    private UserService userService;

    @Autowired
    public TrialBookingController(TrialBookingService trialBookingService, UserService userService) {
        // 建議也在建構子中注入 UserService
        this.trialBookingService = trialBookingService;
        this.userService = userService; // 確保建構子注入
    }

    // 預約體驗課程
    @Operation(summary = "預約體驗課程 (需user/guest)")
    // 權限檢查保持不變，允許 user (已登入) 或 guest (未登入)
    @PreAuthorize("hasAnyAuthority('user', 'guest')")
    @PostMapping("/book")
    // 這裡接收的 bookingRequestDTO 必須包含 booking_name, booking_phone, booking_date, courseId，且現在要包含 email 欄位
    public ResponseEntity<TrialBookingDTO> bookTrialCourse(@RequestBody TrialBookingRequestDTO bookingRequestDTO) {
        logger.info("收到預約體驗課程請求：課程 ID {}，日期 {}，姓名 {}，電話 {}。",
                    bookingRequestDTO.getCourseId(), bookingRequestDTO.getBookingDate(),
                    bookingRequestDTO.getBookingName(), bookingRequestDTO.getBookingPhone());
        // 注意：敏感資訊（如 email）不建議在 info 日誌中完整輸出

        User user = null; // 用來儲存找到的 User 實體，如果是非匿名用戶
        String contactEmail; // 用來儲存本次預約使用的聯絡 email

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        // 檢查使用者是否已認證 (已登入)，並且不是匿名的 Principal
        if (authentication != null && authentication.isAuthenticated() && !authentication.getPrincipal().equals("anonymousUser")) {
             Object principal = authentication.getPrincipal();

             // 根據您的 Spring Security 配置和日誌，Principal 可能是 UserDetails 實作
             if (principal instanceof UserDetails) {
                  UserDetails userDetails = (UserDetails) principal;
                  String usernameFromPrincipal = userDetails.getUsername(); // 獲取使用者名 (通常是郵箱或用戶名)
                  logger.info(" Principal 是 UserDetails 類型。獲取到的用戶名: {}", usernameFromPrincipal);

                  try {
                      // *** 通過 UserService 根據用戶名 (假設是郵箱) 查找 User 實體 ***
                      // 這需要 UserService 有 findByUsername 或 findByEmail 方法
                      Optional<User> userOptional = userService.findByEmail(usernameFromPrincipal); // 假設 username 就是 email
                      if (userOptional.isPresent()) {
                          user = userOptional.get(); // 找到 User 實體
                          contactEmail = user.getEmail(); // 使用註冊使用者的 email
                          logger.info("根據用戶名 '{}' 找到用戶，使用其註冊郵箱: {}", usernameFromPrincipal, contactEmail);
                      } else {
                           logger.warn(" Principal 是 UserDetails 類型，但根據用戶名 '{}' 在資料庫中找不到對應的用戶。", usernameFromPrincipal);
                           // 雖然已認證但找不到對應的資料庫 User，這種情況比較異常，作為匿名處理
                           contactEmail = bookingRequestDTO.getBookingEmail(); // 使用 DTO 中的 email
                           logger.warn(" 作為匿名處理，使用 DTO 中的郵箱: {}", contactEmail);
                      }
                  } catch (Exception e) {
                      logger.error(" 處理 UserDetails Principal 或根據用戶名查找用戶時發生錯誤。", e);
                       // 發生錯誤，作為匿名處理
                      contactEmail = bookingRequestDTO.getBookingEmail(); // 使用 DTO 中的 email
                      logger.warn(" 處理Principal時發生錯誤，作為匿名處理，使用 DTO 中的郵箱: {}", contactEmail);
                  }
             } else {
                  // 如果 Principal 不是 UserDetails 類型，且非匿名，可能是其他自定義 Principal 或配置問題
                  logger.warn(" Principal 是未知類型 {}，無法自動獲取使用者資訊。", principal != null ? principal.getClass().getName() : "null");
                   // 無法獲取使用者資訊，作為匿名處理
                  contactEmail = bookingRequestDTO.getBookingEmail(); // 使用 DTO 中的 email
                  logger.warn(" 作為匿名處理，使用 DTO 中的郵箱: {}", contactEmail);
             }

        } else { // 使用者未認證 (匿名用戶)，或 Principal 是 "anonymousUser"
             logger.info(" 使用者未認證或為匿名用戶。");
             user = null; // user 實體保持 null
             contactEmail = bookingRequestDTO.getBookingEmail(); // 從請求 DTO 中獲取 email
             logger.info(" 從 DTO 中獲取郵箱供匿名預約使用: {}", contactEmail);

             // *** 對匿名用戶提交的 email 進行伺服器端基本驗證 ***
             if (contactEmail == null || contactEmail.trim().isEmpty()) {
                 logger.warn(" 匿名預約請求缺少郵箱。");
                 // 返回 BAD REQUEST 錯誤，因為 email 是必須的 (根據 DB 設計)
                 return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(null); // 或者返回一個包含錯誤訊息的 DTO
             }
             // 可以添加更詳細的 email 格式驗證，例如使用 Regex
             // if (!isValidEmail(contactEmail)) { ... return BAD_REQUEST ... }
        }

        // 在這裡，我們已經確定了：
        // - user 變數：如果是註冊使用者且找到，則包含 User 實體；否則為 null。
        // - contactEmail 變數：如果是註冊使用者，為其註冊 email；如果是匿名者，為從 DTO 獲取的 email。

        logger.info(" 準備將預約資訊傳遞給 Service：使用者 {}，聯絡郵箱 {}，課程 ID {}，日期 {}。",
                     user != null ? "ID=" + user.getId() : "匿名", contactEmail,
                     bookingRequestDTO.getCourseId(), bookingRequestDTO.getBookingDate());


        try {
            // *** 修改 Service 方法呼叫，將 user (nullable) 和 contactEmail 傳遞過去 ***
            // 您需要修改 TrialBookingService 中的 bookTrialCourse 方法簽名和邏輯
            TrialBookingDTO bookedBooking = trialBookingService.bookTrialCourse(
                user, // 傳遞 User 實體 (可能為 null)
                contactEmail, // 傳遞確定的聯絡 email
                bookingRequestDTO // 傳遞其他預約資訊 DTO
            );

            logger.info("體驗預約成功，預約 ID：{}。", bookedBooking.getId());
            // 返回創建成功的狀態碼和預約詳情
            return ResponseEntity.status(HttpStatus.CREATED).body(bookedBooking);

        } catch (EntityNotFoundException e) {
            // 這可能發生在 Service 中找不到課程 ID 或（如果 Service 中需要）找不到匿名訪客帳號
            logger.warn(" 預約失敗：找不到相關課程或其他必要資源。", e);
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(null); // 返回找不到的狀態碼
        } catch (IllegalStateException e) {
            logger.warn(" 預約失敗：業務邏輯錯誤。訊息: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(null); // 返回錯誤請求狀態碼
        } catch (Exception e) {
            logger.error(" 預約體驗課程時發生內部錯誤。", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null); // 返回內部伺服器錯誤狀態碼
        }
    }

    // TODO: isValidEmail 方法可以在 Service 或一個 Utility Class 中實作
    private boolean isValidEmail(String email) {
        // 實現 email 格式驗證邏輯
        // 例如使用 Apache Commons Validator 或簡單的正則表達式
        return email != null && email.matches("^[\\w-\\.]+@([\\w-]+\\.)+[\\w-]{2,4}$");
    }


    // --------------------------------------------------------------------------
    // 以下是其他保持不變的 API 端點 (因為它們主要處理已存在的 TrialBooking 記錄)
    // 您需要確保這些端點在返回 TrialBookingDTO 時，DTO 中包含 email 欄位
    // --------------------------------------------------------------------------

    // 取消體驗預約
    @Operation(summary = "取消體驗預約 (需本人/admin)")
    @PreAuthorize("hasAuthority('admin') or @userSecurity.isTrialBookingOwner(#bookingId)")
    @DeleteMapping("/{bookingId}")
    public ResponseEntity<Void> cancelTrialBooking(@PathVariable Integer bookingId) {
        // ... 保持不變，或根據需要在 Service 中處理權限邏輯 ...
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
    @PreAuthorize("hasAnyAuthority('admin', 'coach') or #userId == authentication.principal.id") // 注意 principal 比較可能需要取得 ID
    @GetMapping("/user/{userId}")
    public ResponseEntity<List<TrialBookingDTO>> getTrialBookingsByUserId(@PathVariable Integer userId) {
         logger.info("收到查詢使用者 ID {} 的所有體驗預約請求。", userId);
        // ... 保持不變， Service 應只返回 user_id 相符的記錄 ...
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
        // ... 保持不變， Service 應返回 course_id 相符的記錄 ...
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
        // ... 保持不變 ...
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
        // ... 保持不變 ...
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
            @RequestParam(required = false) Integer userId // 允許按 userId 篩選
    ) {
        logger.info("收到獲取體驗預約列表請求 - 頁碼: {}, 每頁: {}, 狀態過濾: {}, 課程ID過濾: {}, 使用者ID過濾: {}",
                page, size, bookingStatus, courseId, userId);
        // ... 保持不變 ...
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
    @GetMapping("/search/by-user-name") // 這個註解將 GET 請求到 /api/trial-bookings/search/by-user-name 映射到此方法
    public ResponseEntity<List<TrialBookingDTO>> searchTrialBookingsByUserName(@RequestParam String name) {
         logger.info("收到依會員名稱查詢體驗預約請求，名稱：{}。", name);

         // *** 這裡呼叫 Service 層的方法來執行實際的查詢邏輯 ***
         // 您需要在 TrialBookingService 中實現 searchTrialBookingsByUserName 方法
         try {
            List<TrialBookingDTO> bookings = trialBookingService.searchTrialBookingsByUserName(name);
            logger.info("返回符合名稱 '{}' 的 {} 條體驗預約記錄。", name, bookings.size());
            return ResponseEntity.ok(bookings); // 查詢成功，返回 200 OK 和結果列表
         } catch (Exception e) {
            // 根據 Service 層可能拋出的異常類型，可以更細緻地處理錯誤
            logger.error("依會員名稱查詢體驗預約時發生內部錯誤。", e);
            // 返回 500 Internal Server Error 或其他適合的錯誤狀態碼
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null);
         }
    }
}
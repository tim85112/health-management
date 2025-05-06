package com.healthmanagement.controller.course;

import com.healthmanagement.dto.course.EnrollmentDTO;
import com.healthmanagement.dto.course.EnrollmentStatusUpdateDTO;
import com.healthmanagement.dto.course.ErrorResponse;

import com.healthmanagement.model.member.User;
import com.healthmanagement.security.UserSecurity;
import com.healthmanagement.service.course.EnrollmentService;
import com.healthmanagement.service.member.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.persistence.EntityNotFoundException;
import jakarta.validation.Valid;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@RestController
@RequestMapping("/api/enrollments")
@CrossOrigin(origins = "*")
@Tag(name = "報名管理", description = "報名管理API")
public class EnrollmentController {

	private static final Logger logger = LoggerFactory.getLogger(EnrollmentController.class);

    private final EnrollmentService enrollmentService;
    private final UserService userService;
    private final UserSecurity userSecurity;

    @Autowired
    public EnrollmentController(EnrollmentService enrollmentService, UserService userService, UserSecurity userSecurity) {
        this.enrollmentService = enrollmentService;
        this.userService = userService;
        this.userSecurity = userSecurity;
    }

    @Operation(summary = "報名常規課程 (需user)")
    @PreAuthorize("hasAuthority('user')")
    @PostMapping("/courses/{courseId}")
    public ResponseEntity<?> enrollUserToCourse(@AuthenticationPrincipal UserDetails userDetails, @PathVariable Integer courseId) {
        try {
            String email = userDetails.getUsername();
            // 恢復原邏輯：在 Controller 中根據 email 查找 User，並獲取 userId
            User user = userService.findByEmail(email)
                    .orElseThrow(() -> new IllegalStateException("無法找到對應的使用者"));
            Integer userId = user.getId();

            logger.info("使用者 {} 嘗試報名課程 {}", userId, courseId);
            // 呼叫 Service 方法，傳遞正確的 userId (Integer) 和 courseId (Integer)
            EnrollmentDTO enrollmentDTO = enrollmentService.enrollUserToCourse(userId, courseId);
            logger.info("使用者 {} 報名課程 {} 成功，報名 ID: {}", userId, courseId, enrollmentDTO.getId());
            return new ResponseEntity<>(enrollmentDTO, HttpStatus.CREATED);
        } catch (IllegalStateException e) {
             logger.warn("使用者報名課程失敗: {}", e.getMessage());
            return ResponseEntity.badRequest().body(new ErrorResponse(e.getMessage()));
        } catch (EntityNotFoundException e) {
             logger.error("使用者報名課程 {} 失敗: {}", courseId, e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(new ErrorResponse(e.getMessage()));
        } catch (Exception e) {
             logger.error("使用者報名課程 {} 發生內部錯誤", courseId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(new ErrorResponse("報名失敗: " + e.getMessage()));
        }
    }

    @Operation(summary = "取消常規課程報名 (需本人或 admin/coach)")
    @PreAuthorize("hasAnyAuthority('admin', 'coach') or @userSecurity.isCurrentUserByEnrollmentId(#enrollmentId)")
    @DeleteMapping("/{enrollmentId}")
    public ResponseEntity<?> cancelEnrollment(@PathVariable Integer enrollmentId) {
        try {
            logger.info("嘗試取消報名 ID: {}", enrollmentId);
            enrollmentService.cancelEnrollment(enrollmentId);
            logger.info("報名 ID {} 取消成功。", enrollmentId);
            return ResponseEntity.ok().body(new ErrorResponse("取消成功。"));
        } catch (EntityNotFoundException e) {
             logger.error("取消報名 {} 失敗: 報名記錄未找到", enrollmentId, e);
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(new ErrorResponse(e.getMessage()));
        } catch (SecurityException e) {
             logger.warn("取消報名 {} 失敗: 時間限制或安全例外", enrollmentId, e);
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(new ErrorResponse(e.getMessage()));
        } catch (AccessDeniedException e) {
             logger.warn("取消報名 {} 失敗: 權限不足", enrollmentId, e);
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(new ErrorResponse(e.getMessage()));
        } catch (Exception e) {
             logger.error("取消報名 {} 發生內部錯誤", enrollmentId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(new ErrorResponse("取消失敗: " + e.getMessage()));
        }
    }

    @Operation(summary = "查詢特定使用者的所有報名紀錄 (需本人或 admin/coach)")
    @PreAuthorize("hasAnyAuthority('admin', 'coach') or @userSecurity.isCurrentUser(#userId)")
    @GetMapping("/users/{userId}")
    public ResponseEntity<?> getEnrollmentsByUserId(@PathVariable Integer userId) {
        try {
             logger.info("查詢使用者 {} 的所有報名紀錄...", userId);
             List<EnrollmentDTO> enrollments = enrollmentService.getEnrollmentsByUserId(userId);
             logger.info("找到使用者 {} 的 {} 條報名記錄。", userId, enrollments.size());
             return ResponseEntity.ok(enrollments);
        } catch (EntityNotFoundException e) {
             logger.error("查詢使用者 {} 的報名紀錄失敗: 使用者未找到", userId, e);
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(new ErrorResponse(e.getMessage()));
        } catch (Exception e) {
             logger.error("查詢使用者 {} 的報名紀錄發生內部錯誤", userId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(new ErrorResponse("獲取報名紀錄失敗。"));
        }
    }

    @Operation(summary = "查詢特定課程的所有報名紀錄 (需admin/coach)")
    @PreAuthorize("hasAnyAuthority('admin', 'coach')")
    @GetMapping("/courses/{courseId}")
    public ResponseEntity<?> getEnrollmentsByCourseId(@PathVariable Integer courseId) {
        try {
            logger.info("查詢課程 {} 的所有報名紀錄...", courseId);
             List<EnrollmentDTO> enrollments = enrollmentService.getEnrollmentsByCourseId(courseId);
             logger.info("找到課程 {} 的 {} 條報名記錄。", courseId, enrollments.size());
             return ResponseEntity.ok(enrollments);
        } catch (EntityNotFoundException e) {
             logger.error("查詢課程 {} 的報名紀錄失敗: 課程未找到", courseId, e);
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(new ErrorResponse(e.getMessage()));
        } catch (Exception e) {
             logger.error("查詢課程 {} 的報名紀錄發生內部錯誤", courseId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(new ErrorResponse("獲取報名紀錄失敗。"));
        }
    }

    @Operation(summary = "查詢特定常規課程是否已滿")
    @GetMapping("/courses/{courseId}/is-full")
    public ResponseEntity<?> isCourseFull(@PathVariable Integer courseId) {
        try {
            logger.info("查詢課程 {} 是否已滿...", courseId);
            boolean isFull = enrollmentService.isCourseFull(courseId);
            logger.info("課程 {} 已滿狀態: {}", courseId, isFull);
            return ResponseEntity.ok(isFull);
        } catch (EntityNotFoundException e) {
             logger.error("查詢課程 {} 是否已滿失敗: 課程未找到", courseId, e);
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(new ErrorResponse(e.getMessage()));
        } catch (Exception e) {
             logger.error("查詢課程 {} 是否已滿發生內部錯誤", courseId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(new ErrorResponse("查詢課程是否已滿失敗。"));
        }
    }

    @Operation(summary = "查詢特定使用者是否已報名特定常規課程 (需本人或 admin/coach)")
    @PreAuthorize("hasAnyAuthority('admin', 'coach') or @userSecurity.isCurrentUser(#userId)")
    @GetMapping("/users/{userId}/courses/{courseId}/is-enrolled")
    public ResponseEntity<?> isUserEnrolled(@PathVariable Integer userId, @PathVariable Integer courseId) {
        try {
             logger.info("查詢使用者 {} 是否已報名課程 {}...", userId, courseId);
             boolean isEnrolled = enrollmentService.isUserEnrolled(userId, courseId);
             logger.info("使用者 {} 是否已報名課程 {}: {}", userId, courseId, isEnrolled);
            return ResponseEntity.ok(isEnrolled);
        } catch (EntityNotFoundException e) {
             logger.error("查詢使用者 {} 是否已報名課程 {} 失敗: 用戶或課程未找到", userId, courseId, e);
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(new ErrorResponse(e.getMessage()));
        } catch (Exception e) {
             logger.error("查詢使用者 {} 是否已報名課程 {} 發生內部錯誤", userId, courseId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(new ErrorResponse("查詢使用者是否已報名失敗。"));
        }
    }

    @Operation(summary = "查詢特定常規課程的已報名人數")
    @GetMapping("/courses/{courseId}/count")
    public ResponseEntity<?> getEnrolledCount(@PathVariable Integer courseId) {
        try {
            logger.info("查詢課程 {} 的已報名人數...", courseId);
            int count = enrollmentService.getEnrolledCount(courseId);
            logger.info("課程 {} 的已報名人數: {}", courseId, count);
            return ResponseEntity.ok(count);
        } catch (EntityNotFoundException e) {
             logger.error("查詢課程 {} 的已報名人數失敗: 課程未找到", courseId, e);
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(new ErrorResponse(e.getMessage()));
        } catch (Exception e) {
             logger.error("查詢課程 {} 的已報名人數發生內部錯誤", courseId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(new ErrorResponse("查詢已報名人數失敗。"));
        }
    }

    @Operation(summary = "手動更新報名記錄狀態 (需admin/coach)")
    @PreAuthorize("hasAnyAuthority('admin', 'coach')")
    @PutMapping("/{enrollmentId}")
    public ResponseEntity<?> updateEnrollmentStatus(@PathVariable Integer enrollmentId,
                                                    @Valid @RequestBody EnrollmentStatusUpdateDTO updateDTO) {
        try {
            logger.info("管理員/教練嘗試手動更新報名記錄 ID {} 的狀態為 {}", enrollmentId, updateDTO.getStatus());
            EnrollmentDTO updatedEnrollment = enrollmentService.updateEnrollmentStatus(enrollmentId, updateDTO);
            logger.info("成功手動更新報名記錄 ID {} 的狀態為 {}", enrollmentId, updatedEnrollment.getStatus());
            return ResponseEntity.ok(updatedEnrollment);

        } catch (EntityNotFoundException e) {
            logger.error("手動更新報名記錄 {} 失敗: 未找到記錄. {}", enrollmentId, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(new ErrorResponse(e.getMessage()));
        } catch (IllegalStateException e) {
            logger.warn("手動更新報名記錄 {} 失敗: 無效狀態或業務規則限制. {}", enrollmentId, e.getMessage(), e);
            return ResponseEntity.badRequest().body(new ErrorResponse(e.getMessage()));
        } catch (AccessDeniedException e) {
             logger.warn("手動更新報名記錄 {} 失敗: 權限不足. {}", enrollmentId, e.getMessage(), e);
             return ResponseEntity.status(HttpStatus.FORBIDDEN).body(new ErrorResponse(e.getMessage()));
        } catch (Exception e) {
            logger.error("手動更新報名記錄 {} 發生內部錯誤. {}", enrollmentId, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(new ErrorResponse("更新失敗: " + e.getMessage()));
        }
    }

    @Operation(summary = "查詢報名紀錄 (支援分頁和篩選, 需admin/coach)")
    @PreAuthorize("hasAnyAuthority('admin', 'coach')")
    @GetMapping
    public ResponseEntity<?> getEnrollmentsPaginated(
        @RequestParam(defaultValue = "0") int page,
        @RequestParam(defaultValue = "10") int pageSize,
        @RequestParam(required = false) String status
    ) {
        try {
            logger.info("查詢報名紀錄 - 頁碼: {}, 每頁筆數: {}, 篩選狀態: {}", page, pageSize, status);

            Page<EnrollmentDTO> paginatedResult = enrollmentService.findEnrollmentsPaginated(page, pageSize, status);

            List<EnrollmentDTO> enrollmentsPage = paginatedResult.getContent();
            long total = paginatedResult.getTotalElements();

            logger.info("查詢到 {} 筆報名記錄 (總共 {} 筆)。", enrollmentsPage.size(), total);

            Map<String, Object> responseBody = new HashMap<>();
            responseBody.put("data", enrollmentsPage);
            responseBody.put("total", total);

            return ResponseEntity.ok(responseBody);

        } catch (Exception e) {
            logger.error("查詢報名紀錄發生內部錯誤", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                                 .body(new ErrorResponse("查詢報名紀錄失敗。"));
        }
    }

    @Operation(summary = "依報名編號查詢報名紀錄 (需admin/coach)")
    @PreAuthorize("hasAnyAuthority('admin', 'coach')")
    @GetMapping("/search/by-id/{id}")
    public ResponseEntity<?> searchEnrollmentById(@PathVariable Integer id) {
        try {
            logger.info("嘗試依報名編號查詢報名紀錄: {}", id);
            Optional<EnrollmentDTO> enrollmentDTO = enrollmentService.findEnrollmentById(id);

            if (enrollmentDTO.isPresent()) {
                logger.info("成功找到報名編號 {} 的紀錄", id);
                return ResponseEntity.ok(enrollmentDTO.get());
            } else {
                logger.warn("未找到報名編號 {} 的紀錄", id);
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(new ErrorResponse("未找到該報名紀錄。"));
            }
        } catch (Exception e) {
            logger.error("依報名編號查詢 {} 發生內部錯誤", id, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(new ErrorResponse("查詢失敗: " + e.getMessage()));
        }
    }

    @Operation(summary = "依會員名稱查詢報名紀錄 (需admin/coach)")
    @PreAuthorize("hasAnyAuthority('admin', 'coach')")
    @GetMapping("/search/by-user-name")
    public ResponseEntity<?> searchEnrollmentsByUserName(@RequestParam String name) {
        try {
            logger.info("依會員名稱查詢報名紀錄，名稱: {}", name);

            if (name == null || name.trim().isEmpty()) {
                 return ResponseEntity.badRequest().body(new ErrorResponse("會員名稱不能為空。"));
            }

            List<EnrollmentDTO> enrollments = enrollmentService.searchEnrollmentsByUserName(name.trim());

            logger.info("依會員名稱查詢，找到 {} 條報名記錄。", enrollments.size());

            if (enrollments.isEmpty()) {
                 return ResponseEntity.ok(enrollments);
            } else {
                return ResponseEntity.ok(enrollments);
            }

        } catch (Exception e) {
            logger.error("依會員名稱查詢報名紀錄發生內部錯誤", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(new ErrorResponse("查詢報名紀錄失敗。"));
        }
    }
}
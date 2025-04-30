package com.healthmanagement.controller.course;

import com.healthmanagement.dto.course.CourseRequest;
import com.healthmanagement.dto.course.CourseResponse;
import com.healthmanagement.service.course.CourseService;
import com.healthmanagement.service.course.EnrollmentService; // 引入 EnrollmentService
import com.healthmanagement.service.member.UserService; // 引入 UserService 用於根據郵箱查找使用者

import com.healthmanagement.dto.course.CourseInfoDTO;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.annotation.AuthenticationPrincipal; // 引入 AuthenticationPrincipal
import org.springframework.security.core.userdetails.UserDetails; // 引入 UserDetails
import org.springframework.web.bind.annotation.*;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;

import jakarta.persistence.EntityNotFoundException;
import jakarta.validation.Valid;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Collections;
import java.util.Optional; // 引入 Optional

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@RestController
@RequestMapping("/api/courses")
@CrossOrigin(origins = "*") // 允許 Vue 前端的跨域請求
@Tag(name = "課程管理", description = "課程管理API")
public class CourseController {

	private static final Logger logger = LoggerFactory.getLogger(CourseController.class);

	private final CourseService courseService;
	private final EnrollmentService enrollmentService;
	private final UserService userService;

	@Autowired
	public CourseController(CourseService courseService, EnrollmentService enrollmentService, UserService userService) {
		this.courseService = courseService;
		this.enrollmentService = enrollmentService;
		this.userService = userService;
	}

	// 查詢所有課程，包含使用者的報名/預約狀態和人數
	// 修改以支援分頁和體驗課、星期幾、滿額狀態篩選
	@Operation(summary = "查詢課程列表 (支援分頁及體驗課、星期幾、滿額狀態篩選)")
	@GetMapping
	public ResponseEntity<Page<CourseInfoDTO>> getAllCoursesWithStatus(@AuthenticationPrincipal UserDetails userDetails,
			@Parameter(description = "頁碼 (從 0 開始)") @RequestParam(required = false, defaultValue = "0") Integer page,
			@Parameter(description = "每頁數量") @RequestParam(required = false, defaultValue = "10") Integer size,
			@Parameter(description = "是否為體驗課 (true/false), null 表示不過濾") @RequestParam(required = false) Boolean offersTrialOption,
			@Parameter(description = "星期幾 (0-6), null 表示不過濾") @RequestParam(required = false) Integer dayOfWeek, // 星期幾參數
			@Parameter(description = "滿額狀態 ('full'/'not-full'), null 表示不過濾") @RequestParam(required = false) String fullnessStatus // 新增滿額狀態參數
	) {
		logger.info("收到獲取課程列表請求 - 頁碼: {}, 每頁: {}, 體驗課過濾: {}, 星期幾過濾: {}, 滿額狀態過濾: {}", // 更新日誌
				page, size, offersTrialOption, dayOfWeek, fullnessStatus);

		// 從 UserDetails 中獲取使用者 ID
		Integer userId = null;
		if (userDetails != null) {
			try {
				String identifier = userDetails.getUsername();
				// Assuming userService is correctly injected and has findByEmail
				Optional<com.healthmanagement.model.member.User> userOptional = userService.findByEmail(identifier);

				if (userOptional.isPresent()) {
					userId = userOptional.get().getId();
					logger.debug("已認證使用者 (Principal: {}) 查詢課程列表，UserID: {}", identifier, userId);
				} else {
					logger.warn("已認證使用者 (Principal: {}) 找不到對應 User 實體，無法查詢個人狀態。", identifier);
				}
			} catch (Exception e) {
				logger.error("從 UserDetails 獲取使用者 ID 失敗。", e);
			}
		} else {
			logger.debug("匿名使用者查詢課程列表 (不含個人狀態)。");
		}

		try {
			// 調用 EnrollmentService 中包含使用者狀態的方法，並傳入分頁和篩選參數
			// 現在傳入 fullnessStatus 參數
			Page<CourseInfoDTO> coursePage = enrollmentService.getAllCoursesWithUserStatus(userId, page, size,
					offersTrialOption, dayOfWeek, fullnessStatus);
			logger.info("返回符合篩選條件的 {} 個課程 (共 {} 頁)。", coursePage.getTotalElements(), coursePage.getTotalPages());
			return ResponseEntity.ok(coursePage); // 返回 Page<CourseInfoDTO>
		} catch (Exception e) {
			logger.error("獲取課程列表時發生錯誤。", e);
			// 您可能希望根據錯誤類型返回不同的 HTTP 狀態碼
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Page.empty()); // 返回空分頁結果
		}
	}

	// 依照課程ID查詢。
	@Operation(summary = "依照課程ID查詢")
	@GetMapping("/{id}")
	public ResponseEntity<CourseResponse> getCourseById(@PathVariable Integer id) {
		logger.info("收到依課程 ID 查詢請求：{}", id);
		try {
			CourseResponse course = courseService.getCourseById(id);
			logger.info("返回課程 ID: {}。", id);
			return ResponseEntity.ok(course);
		} catch (EntityNotFoundException e) {
			logger.warn("找不到課程 ID: {}。", id, e);
			return ResponseEntity.status(HttpStatus.NOT_FOUND).body(null);
		} catch (Exception e) {
			logger.error("查詢課程 ID {} 時發生錯誤。", id, e);
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null);
		}
	}

	// 新增課程 (需admin)
	@Operation(summary = "新增課程 (需admin)")
	@PreAuthorize("hasAuthority('admin')")
	@PostMapping
	public ResponseEntity<CourseResponse> createCourse(@Valid @RequestBody CourseRequest courseRequest) {
		logger.info("收到創建課程請求：{}", courseRequest.getName());
		try {
			CourseResponse createdCourse = courseService.createCourse(courseRequest);
			logger.info("課程創建成功，ID: {}。", createdCourse.getId());
			return ResponseEntity.status(HttpStatus.CREATED).body(createdCourse);
		} catch (Exception e) {
			logger.error("創建課程 {} 時發生錯誤。", courseRequest.getName(), e);
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null);
		}
	}

	// 修改課程 (需admin)
	@Operation(summary = "修改課程 (需admin)")
	@PreAuthorize("hasAuthority('admin')")
	@PutMapping("/{id}")
	public ResponseEntity<CourseResponse> updateCourse(@PathVariable Integer id,
			@RequestBody CourseRequest courseRequest) {
		logger.info("收到更新課程 ID 為 {} 的請求。", id);
		try {
			CourseResponse updatedCourse = courseService.updateCourse(id, courseRequest);
			logger.info("課程 ID {} 更新成功。", id);
			return ResponseEntity.ok(updatedCourse);
		} catch (EntityNotFoundException e) {
			logger.warn("嘗試更新課程 ID 為 {}，但未找到。", id, e);
			return ResponseEntity.status(HttpStatus.NOT_FOUND).body(null);
		} catch (Exception e) {
			logger.error("更新課程 ID {} 時發生錯誤。", id, e);
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null);
		}
	}

	// 刪除課程 (Service 層已包含報名檢查)
	@Operation(summary = "刪除課程 (需admin)")
	@PreAuthorize("hasAuthority('admin')")
	@DeleteMapping("/{id}")
	public ResponseEntity<Void> deleteCourse(@PathVariable Integer id) {
		logger.info("收到刪除課程 ID 為 {} 的請求。", id);
		try {
			courseService.deleteCourse(id);
			logger.info("課程 ID {} 刪除成功。", id);
			return ResponseEntity.noContent().build();
		} catch (EntityNotFoundException e) {
			logger.warn("嘗試刪除課程 ID 為 {}，但未找到。", id, e);
			return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
		} catch (IllegalStateException e) {
			logger.warn("由於存在活躍報名/預約，無法刪除課程 ID {}。", id, e);
			return ResponseEntity.status(HttpStatus.CONFLICT).body(null);
		} catch (Exception e) {
			logger.error("刪除課程 ID {} 時發生錯誤。", id, e);
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null);
		}
	}

	// 依照課程名稱查詢。
	@Operation(summary = "依照課程名稱查詢")
	@GetMapping("/course_search")
	public ResponseEntity<List<CourseResponse>> searchCoursesByCourseName(@RequestParam String name) {
		logger.info("收到依課程名稱查詢請求：{}", name);
		List<CourseResponse> courses = courseService.searchCoursesByCourseName(name);
		logger.info("返回 {} 個匹配名稱 '{}' 的課程。", courses.size(), name);
		return ResponseEntity.ok(courses);
	}

	// 依照教練ID查詢。
	@Operation(summary = "依照教練ID查詢")
	@GetMapping("/coach")
	public ResponseEntity<Page<CourseResponse>> findByCoachId(@RequestParam Integer coachId,
			@PageableDefault(size = 10) Pageable pageable) { // size = 10 是預設每頁大小
		logger.info("收到依教練 ID 查詢請求 (分頁)：{}。頁碼: {}, 每頁大小: {}", coachId, pageable.getPageNumber(),
				pageable.getPageSize());
		Page<CourseResponse> coursesPage = courseService.findByCoachId(coachId, pageable);

		logger.info("返回教練 ID {} 的課程 (分頁)。總數: {}，總頁數: {}", coachId, coursesPage.getTotalElements(),
				coursesPage.getTotalPages());
		return ResponseEntity.ok(coursesPage);
	}

	// 依照教練名字查詢。
	@Operation(summary = "依照教練名字查詢")
	@GetMapping("/coach_search")
	public ResponseEntity<List<CourseResponse>> searchCoursesByCoachName(@RequestParam String coachName) {
		logger.info("收到依教練名稱查詢請求：{}", coachName);
		List<CourseResponse> courses = courseService.searchCoursesByCoachName(coachName);
		logger.info("返回 {} 個匹配教練名稱 '{}' 的課程。", courses.size(), coachName);
		return ResponseEntity.ok(courses);
	}

	// 查詢特定星期幾的課程。
	@Operation(summary = "依照星期查詢")
	@GetMapping("/day/{dayOfWeek}")
	public ResponseEntity<List<CourseResponse>> getCoursesByDayOfWeek(@PathVariable Integer dayOfWeek) {
		logger.info("收到依星期查詢課程請求：{}", dayOfWeek);
		List<CourseResponse> courses = courseService.getCoursesByDayOfWeek(dayOfWeek);
		logger.info("返回星期 {} 的 {} 個課程。", dayOfWeek, courses.size());
		return ResponseEntity.ok(courses);
	}

	// 依照時段查詢課程的端點
	@Operation(summary = "依照日期時間範圍查詢課程")
	@GetMapping("/date-time-range")
	public ResponseEntity<List<CourseResponse>> getCoursesByDateTimeRange(@RequestParam LocalDateTime startTime,
			@RequestParam LocalDateTime endTime) {
		logger.info("收到依日期時間範圍查詢課程請求：{} 到 {}。", startTime, endTime);
		// 調用 Service 方法，現在傳入 LocalDateTime
		List<CourseResponse> courses = courseService.getCoursesByDateTimeRange(startTime, endTime);
		logger.info("返回日期時間範圍 {} 到 {} 的 {} 個課程。", startTime, endTime, courses.size());
		return ResponseEntity.ok(courses);
	}
}
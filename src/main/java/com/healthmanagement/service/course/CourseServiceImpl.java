package com.healthmanagement.service.course;

import com.healthmanagement.dao.course.CourseDAO;
import com.healthmanagement.dao.course.TrialBookingDAO;
import com.healthmanagement.dao.member.UserDAO;
import com.healthmanagement.dao.course.EnrollmentDAO;

import com.healthmanagement.dto.course.CourseRequest;
import com.healthmanagement.dto.course.CourseResponse;
import com.healthmanagement.dto.course.CourseImageDTO; // 確保已引入 CourseImageDTO
import com.healthmanagement.model.course.Course;
import com.healthmanagement.model.course.CourseImage; // 確保已引入 CourseImage
import com.healthmanagement.model.member.User;
import com.healthmanagement.model.course.Enrollment;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

import java.time.temporal.ChronoUnit;
import java.time.temporal.TemporalAdjusters;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.ArrayList; // 引入 ArrayList
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Set; // 引入 Set
import java.util.HashSet; // 引入 HashSet

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Service
public class CourseServiceImpl implements CourseService {

    private static final Logger logger = LoggerFactory.getLogger(CourseServiceImpl.class);

    private final CourseDAO courseDAO;
    private final TrialBookingDAO trialBookingDAO;
    private final EnrollmentService enrollmentService;
    private final UserDAO userDAO;
    private final EnrollmentDAO enrollmentDAO;
    private final EntityManager entityManager; // 初始化
    // 常規和體驗預約的非活躍狀態 (需要與 EnrollmentService/TrialBookingService 一致)
    private static final List<String> INACTIVE_ENROLLMENT_STATUSES = List.of("已取消", "已完成", "未到場");
    private static final List<String> INACTIVE_TRIAL_STATUSES = List.of("已取消", "已完成", "未到場");

    @Autowired
    public CourseServiceImpl(CourseDAO courseDAO, TrialBookingDAO trialBookingDAO,
                             EnrollmentService enrollmentService, UserDAO userDAO,
                             EnrollmentDAO enrollmentDAO,
                             EntityManager entityManager) {
        this.courseDAO = courseDAO;
        this.trialBookingDAO = trialBookingDAO;
        this.enrollmentService = enrollmentService;
        this.userDAO = userDAO;
        this.enrollmentDAO = enrollmentDAO;
        this.entityManager = entityManager;
    }

    // 輔助方法：將 Course 實體轉換為 CourseResponse DTO (這個方法已經處理圖片列表，無需修改)
    private CourseResponse convertToCourseResponse(Course course, Integer bookedTrialCount) {
        if (course == null) {
            return null;
        }

        // 確保 coach 對象被載入 (如果需要 coachName)。強烈建議在 DAO 層通過 @EntityGraph 或 JOIN FETCH 載入。
        User coach = course.getCoach();
        Integer coachId = (coach != null) ? coach.getId() : null;
        String coachName = (coach != null) ? coach.getName() : "N/A";
        // 處理圖片列表的邏輯。強烈建議在 DAO 層通過 @EntityGraph 或 JOIN FETCH 載入 images 關聯。
        List<CourseImageDTO> imageDtoList = convertToImageDtoList(course.getImages());
        // 使用 CourseResponse 的 Builder 構建 DTO
        CourseResponse response = CourseResponse.builder()
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
                .registeredCount(enrollmentService.getEnrolledCount(course.getId())) // 計算常規已報名人數
                .bookedTrialCount(bookedTrialCount != null ? bookedTrialCount : 0) // 設置已預約體驗人數
                // 計算額滿狀態
                .full(isCourseFull(course.getId())) // 使用 CourseService 中的方法
                .trialFull(isCourseTrialFull(course.getId(), bookedTrialCount != null ? bookedTrialCount : 0)) // 使用 CourseService 中的方法
                // userStatus 和 userTrialBookingStatus 通常在 Controller 層根據當前用戶填充，Service 層可能無法獲知
                .userStatus(null)
                .userTrialBookingStatus(null)
                // 將轉換好的圖片列表設置到 DTO 中
                .images(imageDtoList)
                .build();

        return response;
    }

	// 輔助方法：將 List<CourseImage> 轉換為 List<CourseImageDTO>
    private List<CourseImageDTO> convertToImageDtoList(List<CourseImage> images) {
        if (images == null || images.isEmpty()) {
            return Collections.emptyList();
        }
        // 使用 Stream 將 List<CourseImage> 轉換為 List<CourseImageDTO>
        return images.stream()
             .sorted(Comparator.comparing(CourseImage::getImageOrder, Comparator.nullsLast(Comparator.naturalOrder())))
            .map(image -> CourseImageDTO.builder()
                .id(image.getId()) // 在 DTO 中包含 id，編輯時會用到
                .imageUrl(image.getImageUrl())
                .imageOrder(image.getImageOrder())
                .build())
            .collect(Collectors.toList());
    }

    // 輔助方法：計算課程的下一個發生日期和時間，相對於當前日期時間
    // 假設 DayOfWeek 是 0=Sun, 6=Sat
    private LocalDateTime calculateNextCourseOccurrenceTime(Course course) {
         if (course == null || course.getDayOfWeek() == null || course.getStartTime() == null) {
              logger.warn("課程 ID {} 有不完整的排程資訊 (dayOfWeek: {}, startTime: {})。無法計算下一個發生。",
                          course != null ? course.getId() : "N/A",
                          course != null ? course.getDayOfWeek() : "N/A", course != null ? course.getStartTime() : "N/A");
             return null;
         }
         LocalDate today = LocalDate.now();
         LocalTime nowTime = LocalTime.now();
         // 將資料庫的 0-6 (Sun-Sat) 轉換為 Java 的 DayOfWeek (1-7, Mon-Sun)
         // 或者根據你的資料庫實際定義調整轉換邏輯
         int dbDayOfWeek = course.getDayOfWeek();
         DayOfWeek courseDayOfWeek = DayOfWeek.of((dbDayOfWeek + 1) % 7 == 0 ? 7 : (dbDayOfWeek + 1) % 7);
         DayOfWeek todayDayOfWeek = today.getDayOfWeek();

         LocalDate nextDate = today;
         int daysUntilNext = courseDayOfWeek.getValue() - todayDayOfWeek.getValue();
         if (daysUntilNext < 0) {
             daysUntilNext += 7;
         }
         if (daysUntilNext == 0 && course.getStartTime().isBefore(nowTime)) {
             daysUntilNext = 7;
         }
         nextDate = today.plusDays(daysUntilNext);
         return LocalDateTime.of(nextDate, course.getStartTime());
     }

    // 輔助方法：批量獲取提供體驗選項的課程的下一個排程的 bookedTrialCount
    @Transactional(readOnly = true)
    public Map<Integer, Integer> getNextOccurrenceBookedTrialCounts(List<Course> courses) {
        logger.info("正在為 {} 個課程獲取下一個排程的體驗預約計數...", courses != null ? courses.size() : 0);
        Map<Integer, Integer> countsMap = new HashMap<>();

        if (courses == null || courses.isEmpty()) {
            logger.warn("傳入的課程列表為空或 null，返回空 Map。");
            return countsMap; // 返回空的 Map
        }

        for (Course course : courses) {
             if (course == null) {
                 logger.warn("課程列表包含 null 項目，跳過處理。");
                 continue; // 跳過 null 課程
             }
            // 只為提供體驗選項且最大體驗容量大於 0 的課程計算計數
            if (course.getOffersTrialOption() != null && course.getOffersTrialOption() &&
                course.getMaxTrialCapacity() != null && course.getMaxTrialCapacity() > 0) {

                try {
                    // 1. 計算當前課程的下一個排程時間
                    LocalDateTime nextOccurrenceTime = calculateNextCourseOccurrenceTime(course);
                    logger.debug("課程 ID {} ({} {}) 的下一個排程時間計算為: {}", course.getId(), course.getDayOfWeek(), course.getStartTime(), nextOccurrenceTime);
                    if (nextOccurrenceTime != null) {
                        // 2. 查詢該課程在該下一個排程時間的活躍體驗預約人數
                        int bookedTrialCount = trialBookingDAO.countTrialBookingsByCourseDateActualStartTimeAndStatusNotInNative(
                            course.getId(),
                            nextOccurrenceTime.toLocalDate(),
                            nextOccurrenceTime.toLocalTime(),
                            INACTIVE_TRIAL_STATUSES // 傳遞非活躍狀態列表
                        );
                        logger.debug("課程 ID {} 在 {} 體驗預約計數查詢結果: {}", course.getId(), nextOccurrenceTime, bookedTrialCount);
                        // 3. 將計數存入 Map
                        countsMap.put(course.getId(), bookedTrialCount);
                    } else {
                        // 如果無法計算下一個排程時間 (例如課程時間設置有問題)，將計數設為 0
                        logger.warn("無法計算課程 ID {} 的下一個排程時間，將其體驗預約計數設為 0。", course.getId());
                        countsMap.put(course.getId(), 0); // 確保 Map 中有該課程的鍵，避免前端獲取時報錯
                    }
                } catch (Exception e) {
                    // 處理計算或 DAO 呼叫過程中可能發生的異常
                    logger.error("處理課程 ID {} 的體驗預約計數時發生錯誤，將其計數設為 0。", course.getId(), e);
                    countsMap.put(course.getId(), 0); // 確保異常情況下也有計數
                }

            } else {
                // 如果課程不提供體驗選項或最大體驗容量為 0，計數就是 0
                countsMap.put(course.getId(), 0);
            }
        }

        logger.info("完成獲取體驗預約計數，返回 Map 大小: {}", countsMap.size());
        return countsMap; // 返回包含所有課程體驗預約計數的 Map
    }

    @Override
    @Transactional(readOnly = true)
    public List<CourseResponse> getAllCourses() {
        logger.info("獲取所有課程並轉換為 CourseResponse。");
        // 呼叫 CourseDAO 中新增的 findAllWithImagesAndCoach() 方法
        List<Course> courses = courseDAO.findAllWithImagesAndCoach();
        // 獲取提供體驗選項的課程的下一個排程的 bookedTrialCounts
        Map<Integer, Integer> bookedTrialCountsMap = getNextOccurrenceBookedTrialCounts(courses);
        // 將 Course 實體列表轉換為 CourseResponse 列表
        List<CourseResponse> responseList = courses.stream()
             .map(course -> convertToCourseResponse(course, bookedTrialCountsMap.getOrDefault(course.getId(), 0)))
             .collect(Collectors.toList());
        logger.info("返回 {} 個課程作為 CourseResponse。", responseList.size());
        return responseList;
    }

    @Override
    @Transactional(readOnly = true)
    public CourseResponse getCourseById(Integer id) {
        logger.info("獲取課程 ID: {}", id);
        // 呼叫 CourseDAO 中覆寫的 findById(Integer id) 方法並加上 @EntityGraph({"images", "coach"})
        Optional<Course> optional = courseDAO.findById(id);
        if (optional.isEmpty()) {
            logger.warn("找不到課程 ID: {}", id);
            throw new EntityNotFoundException("找不到課程 ID: " + id);
        }
        Course course = optional.get();
        // 獲取常規課程的已報名人數
        int registeredCount = enrollmentService.getEnrolledCount(course.getId());
        logger.info("課程 ID {} 的常規已報名人數為: {}", course.getId(), registeredCount);
        // 獲取單個課程的下一個排程的 bookedTrialCount (如果提供體驗選項)
        Integer bookedTrialCount = 0;
        boolean courseOffersTrial = course.getOffersTrialOption() != null && course.getOffersTrialOption();
        if (courseOffersTrial) {
            List<Course> singleCourseList = Collections.singletonList(course);
            logger.debug("課程 ID {} 提供體驗，準備呼叫 getNextOccurrenceBookedTrialCounts 獲取體驗人數。", course.getId());
            // 呼叫共用的 getNextOccurrenceBookedTrialCounts 方法
            Map<Integer, Integer> countsMap = getNextOccurrenceBookedTrialCounts(singleCourseList);
            bookedTrialCount = countsMap.getOrDefault(course.getId(), 0);
            if (bookedTrialCount == null) {
                 bookedTrialCount = 0;
            }
        }
        logger.info("課程 ID {} 在 getCourseById 中獲取的最終 bookedTrialCount 為: {}", course.getId(), bookedTrialCount);
        // 使用 convertToCourseResponse 構建 CourseResponse DTO
        CourseResponse response = convertToCourseResponse(course, bookedTrialCount);
        logger.info("課程 ID {} 的 CourseResponse DTO (含人數/狀態/圖片) 構建完成。", course.getId());
        return response;
    }

    @Override
    @Transactional(readOnly = true)
    public Course getById(Integer id) {
        // 這個方法通常用於內部，返回 Entity
        logger.info("獲取課程 Entity ID: {}", id);
        // 注意：這個方法返回 Course Entity，如果外部調用者在事務外部訪問 Course 的 images 列表，
        // 且 images 是 LAZY 載入的，可能會引發 LazyInitializationException。
        // 這裡使用 CourseDAO 中覆寫的 findById(Integer id) 方法並加上 @EntityGraph({"images", "coach"})
        Optional<Course> optional = courseDAO.findById(id);
        if (optional.isEmpty()) {
            logger.warn("找不到課程 Entity ID: {}", id);
            throw new EntityNotFoundException("找不到課程 Entity ID: " + id);
        }
        return optional.get();
    }


    @Override
    @Transactional
    public CourseResponse createCourse(CourseRequest courseRequest) {
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
        // 從 CourseRequest 設置 offersTrialOption 和 maxTrialCapacity
        course.setOffersTrialOption(courseRequest.getOffersTrialOption() != null ? courseRequest.getOffersTrialOption() : false);
        course.setMaxTrialCapacity(courseRequest.getMaxTrialCapacity());

        // ====== START: 添加處理圖片新增的邏輯 ======
        if (courseRequest.getImages() != null && !courseRequest.getImages().isEmpty()) {
            List<CourseImage> courseImages = new ArrayList<>();
            // 對圖片列表按 imageOrder 進行排序，確保處理順序與前端一致
            List<CourseImageDTO> sortedImages = courseRequest.getImages().stream()
                                                    .sorted(Comparator.comparing(CourseImageDTO::getImageOrder, Comparator.nullsLast(Comparator.naturalOrder())))
                                                    .collect(Collectors.toList());

            for (int i = 0; i < sortedImages.size(); i++) {
                CourseImageDTO imageDTO = sortedImages.get(i);
                // 注意：這裡假設 CourseImageDTO 中的 imageUrl 已經是圖片上傳後的 URL。
                // 實際應用中，如果前端傳送的是檔案，檔案上傳和 URL 生成的邏輯應在此之前或由單獨的服務處理。

                // 只處理 imageUrl 不為空的情況
                if (imageDTO.getImageUrl() != null && !imageDTO.getImageUrl().trim().isEmpty()) {
                    CourseImage courseImage = new CourseImage();
                    // ID 不設定，讓資料庫自動生成
                    courseImage.setImageUrl(imageDTO.getImageUrl().trim());
                    // 使用 DTO 中的順序，如果 DTO 沒有提供，可以使用排序後的索引 i 作為預設值
                    courseImage.setImageOrder(imageDTO.getImageOrder() != null ? imageDTO.getImageOrder() : i);
                    courseImage.setCourse(course); // 建立關聯

                    courseImages.add(courseImage);
                    logger.debug("創建 CourseImage Entity: imageUrl={}, imageOrder={}", courseImage.getImageUrl(), courseImage.getImageOrder());
                } else {
                    logger.warn("創建課程時，圖片 DTO 的 imageUrl 為空或空白，忽略此圖片。");
                }
            }
            // 設定圖片列表到 Course Entity
            if (!courseImages.isEmpty()) {
                 course.setImages(courseImages);
            } else {
                 // 如果沒有有效圖片，確保圖片列表為空列表，而不是 null
                 course.setImages(new ArrayList<>());
            }

        } else {
            // 如果請求中沒有圖片列表或圖片列表為空，確保圖片列表為空列表
            course.setImages(new ArrayList<>());
            logger.info("課程 {} 沒有圖片。", courseRequest.getName());
        }
        // ====== END: 添加處理圖片新增的邏輯 ======


        Course savedCourse = courseDAO.save(course);
        // 如果 Course 的 images 關聯設置了 CascadeType.PERSIST 或 ALL，CourseImage 會自動儲存

        logger.info("課程創建成功，ID: {}。", savedCourse.getId());
        // 獲取並返回包含圖片的完整 CourseResponse
        // 假設 findById 返回的 Course Entity 的 images 列表已經被載入 (通過 @EntityGraph 或 JOIN FETCH)
        Course fullCourse = courseDAO.findById(savedCourse.getId()).orElse(savedCourse);
        // 新增的課程 bookedTrialCount 為 0
        return convertToCourseResponse(fullCourse, 0);
    }

    @Override
    @Transactional
    public CourseResponse updateCourse(Integer id, CourseRequest courseRequest) {
        logger.info("更新課程 ID: {}", id);
        // 呼叫 CourseDAO 中覆寫的 findById(Integer id) 方法並加上 @EntityGraph({"images", "coach"})
        // 以確保載入圖片列表
        Optional<Course> optional = courseDAO.findById(id);
        if (optional.isEmpty()) {
            logger.warn("嘗試更新課程 ID 為 {}，但未找到。", id);
            throw new EntityNotFoundException("找不到課程 ID: " + id);
        }
        Course existingCourse = optional.get();
        logger.debug("找到現有課程 ID {}。", id);

        // 添加更新課程的業務驗證
        validateCourseRequest(courseRequest);
        // TODO: 驗證：如果 maxTrialCapacity 被減少，需要檢查是否小於當前已有的 bookedTrialCount

        existingCourse.setName(courseRequest.getName());
        existingCourse.setDescription(courseRequest.getDescription());
        existingCourse.setDayOfWeek(courseRequest.getDayOfWeek());
        existingCourse.setStartTime(courseRequest.getStartTime());
        // 獲取 Coach 的引用
        User coachRef = entityManager.getReference(User.class, courseRequest.getCoachId());
        existingCourse.setCoach(coachRef);
        existingCourse.setDuration(courseRequest.getDuration());
        existingCourse.setMaxCapacity(courseRequest.getMaxCapacity());
        // 從 CourseRequest 更新 offersTrialOption 和 maxTrialCapacity
        existingCourse.setOffersTrialOption(courseRequest.getOffersTrialOption() != null ? courseRequest.getOffersTrialOption() : false);
        existingCourse.setMaxTrialCapacity(courseRequest.getMaxTrialCapacity());

        // ====== START: 添加處理圖片更新的邏輯 (新增/刪除/修改順序) ======
        // 獲取當前已有的圖片集合 (確保已載入)
        List<CourseImage> existingImages = existingCourse.getImages();
        // 獲取請求中的圖片 DTO 列表
        List<CourseImageDTO> updatedImageDTOs = courseRequest.getImages();

        // 如果請求中沒有圖片列表，則表示刪除所有現有圖片
        if (updatedImageDTOs == null) {
            updatedImageDTOs = Collections.emptyList();
        }

        // 創建一個 Set 來存放需要保留或更新的現有圖片的 ID
        Set<Integer> imageIdsToKeep = new HashSet<>();
        List<CourseImage> imagesToKeepOrUpdate = new ArrayList<>();

        // 處理請求中的圖片 DTO 列表
        // 對傳入的 DTO 列表按 imageOrder 進行排序，確保處理順序與前端一致
         List<CourseImageDTO> sortedUpdatedImages = updatedImageDTOs.stream()
                                                        .sorted(Comparator.comparing(CourseImageDTO::getImageOrder, Comparator.nullsLast(Comparator.naturalOrder())))
                                                        .collect(Collectors.toList());


        for (int i = 0; i < sortedUpdatedImages.size(); i++) {
            CourseImageDTO imageDTO = sortedUpdatedImages.get(i);
            // 檢查 imageUrl 是否有效
            if (imageDTO.getImageUrl() == null || imageDTO.getImageUrl().trim().isEmpty()) {
                 logger.warn("更新課程 ID {} 時，圖片 DTO 的 imageUrl 為空或空白，忽略此圖片。", id);
                 continue; // 跳過無效圖片
            }

            // 如果 DTO 包含 id，表示是現有圖片
            if (imageDTO.getId() != null) {
                // 查找對應的現有 CourseImage Entity
                Optional<CourseImage> existingImageOptional = existingImages.stream()
                        .filter(img -> img.getId() != null && img.getId().equals(imageDTO.getId()))
                        .findFirst();
                if (existingImageOptional.isPresent()) {
                    // 找到了現有圖片，更新其屬性（例如排序和 URL）
                    CourseImage existingImage = existingImageOptional.get();
                    // 檢查 URL 是否有變化，如果有變化可能表示是替換圖片 (但目前前端流程是刪除舊的再新增)
                    // 這裡簡單更新 URL 和排序
                    existingImage.setImageUrl(imageDTO.getImageUrl().trim()); // 更新 URL
                    existingImage.setImageOrder(imageDTO.getImageOrder() != null ? imageDTO.getImageOrder() : i); // 更新排序
                    imagesToKeepOrUpdate.add(existingImage); // 添加到更新列表
                    imageIdsToKeep.add(existingImage.getId()); // 記錄下這個 ID 需要保留
                    logger.debug("更新現有圖片 ID {}：imageUrl={}, newOrder={}", existingImage.getId(), existingImage.getImageUrl(), existingImage.getImageOrder());
                } else {
                    // DTO 有 ID，但在現有圖片中找不到，這可能是無效的 ID 或嘗試新增帶 ID 的圖片
                    // 視為新增圖片，但不應該帶 ID。這裡為了簡單處理，也創建一個新的 CourseImage。
                    // 更好的做法是在前端或 Controller 層驗證 DTO 的 ID 是否有效。
                    logger.warn("更新課程 ID {} 時，圖片 DTO 帶有 ID ({}) 但在現有圖片中找不到。將其視為新的圖片。", id, imageDTO.getId());
                    CourseImage newImage = new CourseImage();
                    // newImage.setId(incomingImageDTO.getId()); // 新增時不設 ID
                    newImage.setImageUrl(imageDTO.getImageUrl().trim());
                    newImage.setImageOrder(imageDTO.getImageOrder() != null ? imageDTO.getImageOrder() : i);
                    newImage.setCourse(existingCourse); // 建立關聯
                    imagesToKeepOrUpdate.add(newImage); // 添加到更新列表
                    logger.debug("創建新的圖片 (來自帶 ID 的 DTO)：imageUrl={}, imageOrder={}", newImage.getImageUrl(), newImage.getImageOrder());
                }
            } else {
                // 如果 DTO 沒有 ID，表示是新的圖片
                CourseImage newImage = new CourseImage();
                newImage.setImageUrl(imageDTO.getImageUrl().trim());
                newImage.setImageOrder(imageDTO.getImageOrder() != null ? imageDTO.getImageOrder() : i);
                newImage.setCourse(existingCourse); // 建立關聯
                imagesToKeepOrUpdate.add(newImage); // 添加到更新列表
                logger.debug("創建新的圖片 (來自無 ID 的 DTO)：imageUrl={}, imageOrder={}", newImage.getImageUrl(), newImage.getImageOrder());
            }
        }

        // 確定需要刪除的圖片：在現有圖片列表中，但 ID 不在 imageIdsToKeep 中的圖片
        List<CourseImage> imagesToRemove = existingImages.stream()
                .filter(img -> img.getId() != null && !imageIdsToKeep.contains(img.getId()))
                .collect(Collectors.toList());

        // 執行刪除操作
        if (!imagesToRemove.isEmpty()) {
            logger.info("更新課程 ID {} 時，需要刪除 {} 張圖片。", id, imagesToRemove.size());
            // 如果您的 Course Entity 的 images 關聯設置了 CascadeType.REMOVE 或 ALL，
            // 簡單地從集合中移除即可觸發刪除。
            // 假設這裡使用了 orphanRemoval = true 或 CascadeType.REMOVE
            existingImages.removeAll(imagesToRemove); // 從現有集合中移除，觸發刪除

            // 如果沒有設置級聯刪除，您需要一個 CourseImageDAO 或使用 EntityManager 來顯式刪除：
            // for (CourseImage image : imagesToRemove) {
            //     entityManager.remove(image); // 或者使用 CourseImageDAO
            // }
            logger.info("已從集合中移除 {} 張待刪除圖片。", imagesToRemove.size());
        }


        // 將更新後的圖片列表設置回 Course Entity
        // 這一步會將 imagesToKeepOrUpdate 中的圖片（包括新創建的和更新排序的現有圖片）與課程關聯。
        // JPA 會根據 Cascade 設置處理新增和更新。
        // 使用 setImages 可能會清空原來的 PersistentBag 並加入新的，這是常用的更新集合的方式。
        // 確保現有集合被清空再添加，避免重複
         existingCourse.getImages().clear(); // 清空現有集合
         existingCourse.getImages().addAll(imagesToKeepOrUpdate); // 添加更新後的集合

         // 如果您想確保圖片在列表中按照 imageOrder 排序，可以在這裡重新排序集合
         // 但是由於 convertToCourseResponse 在轉換 DTO 時已經排序了，這裡不是必須的
         // existingCourse.getImages().sort(Comparator.comparing(CourseImage::getImageOrder, Comparator.nullsLast(Comparator.naturalOrder())));


        logger.info("課程 ID {} 更新圖片列表，最終包含 {} 張圖片。", existingCourse.getId(), existingCourse.getImages().size());

        // ====== END: 添加處理圖片更新的邏輯 ======

        Course updatedCourse = courseDAO.save(existingCourse); // 儲存更新後的 Course Entity
        logger.info("課程 ID {} 更新成功。", updatedCourse.getId());

        // 獲取更新後的課程下一個排程的 bookedTrialCount (如果提供體驗選項)
         Integer bookedTrialCount = 0;
         if (updatedCourse.getOffersTrialOption() != null && updatedCourse.getOffersTrialOption()) {
            List<Course> singleCourseList = Collections.singletonList(updatedCourse);
            // 呼叫共用的 getNextOccurrenceBookedTrialCounts 方法
            Map<Integer, Integer> countsMap = getNextOccurrenceBookedTrialCounts(singleCourseList);
            bookedTrialCount = countsMap.getOrDefault(updatedCourse.getId(), 0);
             if (bookedTrialCount == null) { // 確保不會返回 null
                 bookedTrialCount = 0;
             }
         }
         logger.debug("更新後課程 ID {} 的 bookedTrialCount 為: {}", updatedCourse.getId(), bookedTrialCount);


        // 獲取並返回包含圖片的完整 CourseResponse
        // 由於級聯保存/更新，圖片已經與 updatedCourse 關聯，直接轉換即可
        // 假設 findById 返回的 Course Entity 的 images 列表已經被載入
        return convertToCourseResponse(courseDAO.findById(updatedCourse.getId()).orElse(updatedCourse), bookedTrialCount);
    }

    // 輔助方法：驗證 CourseRequest
    private void validateCourseRequest(CourseRequest courseRequest) {
         if (courseRequest.getOffersTrialOption() != null && courseRequest.getOffersTrialOption()) {
             if (courseRequest.getMaxTrialCapacity() == null || courseRequest.getMaxTrialCapacity() <= 0) {
                 throw new IllegalArgumentException("如果提供體驗選項，最大體驗人數必須設定且大於 0。");
             }
             // 確保 maxTrialCapacity 不超過 maxCapacity (可選業務規則)
             if (courseRequest.getMaxTrialCapacity() != null && courseRequest.getMaxTrialCapacity() > courseRequest.getMaxCapacity()) {
                 logger.warn("課程 '{}' 提供體驗，但最大體驗人數 ({}) 大於最大常規報名人數 ({})",
                          courseRequest.getName(), courseRequest.getMaxTrialCapacity(), courseRequest.getMaxCapacity());
             }
         } else {
             // 如果不提供體驗選項，理論上不應該設定 maxTrialCapacity
             if (courseRequest.getMaxTrialCapacity() != null && courseRequest.getMaxTrialCapacity() > 0) {
                  logger.warn("課程 '{}' 不提供體驗選項，但最大體驗人數設定為 {}。將忽略此值。", courseRequest.getName(), courseRequest.getMaxTrialCapacity());
                 // 將 DTO 中的 maxTrialCapacity 設為 null，以便在 Entity 中儲存 null 或 0
                  courseRequest.setMaxTrialCapacity(null);
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
    @Transactional
    public void deleteCourse(Integer id) {
        logger.info("刪除課程 ID: {}", id);
        // 檢查是否存在活躍的常規報名或體驗預約
        if (hasActiveEnrollmentsForCourse(id)) { // 呼叫 CourseService 中的方法
             logger.warn("課程 ID {} 存在活躍的常規報名記錄，無法刪除。", id);
            throw new IllegalStateException("課程 ID " + id + " 存在活躍的常規報名記錄，無法刪除。");
        }

        // 檢查是否存在活躍的體驗預約 (直接呼叫 TrialBookingDAO，因為 TrialBookingService 沒有公開檢查單個課程的方法)
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
    public Page<CourseResponse> findByCoachId(Integer coachId, Pageable pageable){
        logger.info("查詢教練 ID {} 的課程 (分頁)。", coachId);
        // 呼叫 CourseDAO 中覆寫的 findByCoachId(Integer coachId, Pageable pageable) 方法並加上 @EntityGraph({"images", "coach"})
		Page<Course> coursePage = courseDAO.findByCoachId(coachId, pageable);
        // 批量獲取這些課程的體驗預約計數
        Map<Integer, Integer> bookedTrialCountsMap = getNextOccurrenceBookedTrialCounts(coursePage.getContent());
        // 使用 Stream 處理課程列表
        return coursePage.map(course -> {
            // 計算常規已報名人數和額滿狀態
            int registeredCount = enrollmentService.getEnrolledCount(course.getId());
            boolean isCourseActuallyFull = isCourseFull(course.getId()); // 使用 CourseService 中的方法

            // 計算體驗預約人數和額滿狀態
            Integer bookedTrialCount = bookedTrialCountsMap.getOrDefault(course.getId(), 0);
            boolean isCourseTrialActuallyFull = isCourseTrialFull(course.getId(), bookedTrialCount); // 使用 CourseService 中的方法

            // 使用 convertToCourseResponse 轉換
            CourseResponse response = convertToCourseResponse(course, bookedTrialCount);
            // 填充常規報名和滿額狀態
            response.setRegisteredCount(registeredCount);
            response.setFull(isCourseActuallyFull);
            // 填充體驗預約和滿額狀態 (已在 convertToCourseResponse 內部處理)
            response.setBookedTrialCount(bookedTrialCount);
            response.setTrialFull(isCourseTrialActuallyFull);
            return response;
        });
	}

    @Override
    @Transactional(readOnly = true)
    public List<CourseResponse> searchCoursesByCourseName(String name) {
        logger.info("依課程名稱查詢：{}", name);
        // 呼叫 CourseDAO 中覆寫的 findByNameContainingIgnoreCase(String name) 方法並加上 @EntityGraph({"images", "coach"})
        List<Course> courses = courseDAO.findByNameContainingIgnoreCase(name);
        logger.info("找到 {} 個匹配名稱 '{}' 的課程。", courses.size(), name);

        // 批量獲取這些課程的體驗預約計數
        Map<Integer, Integer> bookedTrialCountsMap = getNextOccurrenceBookedTrialCounts(courses);
        // 使用 Stream 處理課程列表
        return courses.stream()
             .map(course -> {
                 // 計算常規已報名人數和額滿狀態
                 int registeredCount = enrollmentService.getEnrolledCount(course.getId());
                 boolean isCourseActuallyFull = isCourseFull(course.getId()); // 使用 CourseService 中的方法

                // 計算體驗預約人數和額滿狀態
                 Integer bookedTrialCount = bookedTrialCountsMap.getOrDefault(course.getId(), 0);
                 boolean isCourseTrialActuallyFull = isCourseTrialFull(course.getId(), bookedTrialCount); // 使用 CourseService 中的方法

                 // 使用 convertToCourseResponse 轉換
                 CourseResponse response = convertToCourseResponse(course, bookedTrialCount);
                 // 填充常規報名和滿額狀態
                 response.setRegisteredCount(registeredCount);
                 response.setFull(isCourseActuallyFull);
                 // 填充體驗預約和滿額狀態 (已在 convertToCourseResponse 內部處理)
                 response.setBookedTrialCount(bookedTrialCount);
                 response.setTrialFull(isCourseTrialActuallyFull);

                 return response;
             })
             .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<CourseResponse> searchCoursesByCoachName(String coachName) {
        logger.info("依教練名稱查詢：{}", coachName);
        // 呼叫 CourseDAO 中修改的 findByCoachNameContainingIgnoreCase(String coachName) 方法 (帶 JOIN FETCH)
        List<Course> courses = courseDAO.findByCoachNameContainingIgnoreCase(coachName);
        logger.info("找到 {} 個匹配教練名稱 '{}' 的課程。", courses.size(), coachName);

        // 批量獲取提供體驗選項的課程的下一個排程的 bookedTrialCounts
        Map<Integer, Integer> bookedTrialCountsMap = getNextOccurrenceBookedTrialCounts(courses);
        // 使用 Stream 處理課程列表
        return courses.stream()
             .map(course -> {
                 // 計算常規已報名人數和額滿狀態
                 int registeredCount = enrollmentService.getEnrolledCount(course.getId());
                 boolean isCourseActuallyFull = isCourseFull(course.getId()); // 使用 CourseService 中的方法

                // 計算體驗預約人數和額滿狀態
                 Integer bookedTrialCount = bookedTrialCountsMap.getOrDefault(course.getId(), 0);
                 boolean isCourseTrialActuallyFull = isCourseTrialFull(course.getId(), bookedTrialCount); // 使用 CourseService 中的方法

                 // 使用 convertToCourseResponse 轉換
                 CourseResponse response = convertToCourseResponse(course, bookedTrialCount);
                 // 填充常規報名和滿額狀態
                 response.setRegisteredCount(registeredCount);
                 response.setFull(isCourseActuallyFull);
                 // 填充體驗預約和滿額狀態 (已在 convertToCourseResponse 內部處理)
                 response.setBookedTrialCount(bookedTrialCount);
                 response.setTrialFull(isCourseTrialActuallyFull);

                 return response;
             })
             .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<CourseResponse> getCoursesByDayOfWeek(Integer dayOfWeek) {
        logger.info("依星期查詢課程：{}", dayOfWeek);
        // 呼叫 CourseDAO 中覆寫的 findByDayOfWeek(Integer dayOfWeek) 方法並加上 @EntityGraph({"images", "coach"})
        List<Course> courses = courseDAO.findByDayOfWeek(dayOfWeek);
        logger.info("找到星期 {} 的 {} 個課程。", dayOfWeek, courses.size());
        // 批量獲取提供體驗選項的課程的下一個排程的 bookedTrialCounts
        Map<Integer, Integer> bookedTrialCountsMap = getNextOccurrenceBookedTrialCounts(courses);
        // 轉換並帶入 bookedTrialCount 和圖片列表
        return courses.stream()
             .map(course -> {
                 // 計算常規已報名人數和額滿狀態
                 int registeredCount = enrollmentService.getEnrolledCount(course.getId());
                 boolean isCourseActuallyFull = isCourseFull(course.getId()); // 使用 CourseService 中的方法

                // 計算體驗預約人數和額滿狀態
                 Integer bookedTrialCount = bookedTrialCountsMap.getOrDefault(course.getId(), 0);
                 boolean isCourseTrialActuallyFull = isCourseTrialFull(course.getId(), bookedTrialCount); // 使用 CourseService 中的方法

                 // 使用 convertToCourseResponse 轉換
                 CourseResponse response = convertToCourseResponse(course, bookedTrialCount);
                 // 填充常規報名和滿額狀態
                 response.setRegisteredCount(registeredCount);
                 response.setFull(isCourseActuallyFull);
                 // 填充體驗預約和滿額狀態 (已在 convertToCourseResponse 內部處理)
                 response.setBookedTrialCount(bookedTrialCount);
                 response.setTrialFull(isCourseTrialActuallyFull);

                 return response;
             })
             .collect(Collectors.toList());
    }

    // 依日期時段查詢課程
    @Override
    @Transactional(readOnly = true)
    public List<CourseResponse> getCoursesByDateTimeRange(LocalDateTime startTime, LocalDateTime endTime) {
        logger.info("依日期時段查詢課程服務：查詢範圍從 {} 到 {}。", startTime, endTime);
        // 1. 從資料庫獲取所有課程 (確保載入圖片和教練)
        // 呼叫 CourseDAO 中新增的 findAllWithImagesAndCoach() 方法
        List<Course> allCourses = courseDAO.findAllWithImagesAndCoach();
        logger.info("獲取所有 {} 門課程，準備進行日期時段過濾。", allCourses.size());

        // 2. 在 Service 層過濾課程，判斷其是否在給定的日期時間範圍內發生過
        List<Course> filteredCourses = allCourses.stream()
            .filter(course -> doesCourseOccurInRange(course, startTime, endTime))
            .collect(Collectors.toList());
        logger.info("過濾後找到 {} 門在指定日期時段內發生過的課程。", filteredCourses.size());

        // 3. 獲取過濾後課程的下一個排程的 bookedTrialCounts
        Map<Integer, Integer> bookedTrialCountsMap = getNextOccurrenceBookedTrialCounts(filteredCourses);
        // 4. 轉換並帶入 bookedTrialCount 和圖片列表
        return filteredCourses.stream()
             .map(course -> {
                  // 計算常規已報名人數和額滿狀態
                 int registeredCount = enrollmentService.getEnrolledCount(course.getId());
                 boolean isCourseActuallyFull = isCourseFull(course.getId()); // 使用 CourseService 中的方法

                // 計算體驗預約人數和額滿狀態
                 Integer bookedTrialCount = bookedTrialCountsMap.getOrDefault(course.getId(), 0);
                 boolean isCourseTrialActuallyFull = isCourseTrialFull(course.getId(), bookedTrialCount); // 使用 CourseService 中的方法

                 // 使用 convertToCourseResponse 轉換
                 CourseResponse response = convertToCourseResponse(course, bookedTrialCount);
                 // 填充常規報名和滿額狀態
                 response.setRegisteredCount(registeredCount);
                 response.setFull(isCourseActuallyFull);
                 // 填充體驗預約和滿額狀態 (已在 convertToCourseResponse 內部處理)
                 response.setBookedTrialCount(bookedTrialCount);
                 response.setTrialFull(isCourseTrialActuallyFull);

                 return response;
             })
             .collect(Collectors.toList());
    }

    // 輔助方法：判斷一個課程在給定的日期時間範圍內是否有發生
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
        // 假設你的資料庫是 0=Sun, 6=Sat
        DayOfWeek courseDayOfWeek;
        try {
            // 將 0-6 (Sun-Sat) 轉換為 Java 的 1-7 (Mon-Sun)
            courseDayOfWeek = DayOfWeek.of((course.getDayOfWeek() + 1) % 7 == 0 ? 7 : (course.getDayOfWeek() + 1) % 7);
        } catch (Exception e) {
            logger.warn("課程 ID {} 的星期幾值無效: {}.", course.getId(), course.getDayOfWeek());
            return false;
        }

        LocalTime courseStartTime = course.getStartTime();
        LocalDate currentCheckDate = rangeStart.toLocalDate();
        // 找到第一個課程發生的日期，從 rangeStart 或之後開始計算
        // 如果 rangeStart 是課程日，且開始時間在 rangeStart 之後或等於，就從 rangeStart 的課程日開始
        // 否則從下一個課程日開始
         LocalDateTime firstPossibleOccurrence = LocalDateTime.of(currentCheckDate.with(TemporalAdjusters.nextOrSame(courseDayOfWeek)), courseStartTime);
         // 如果計算出的第一個可能發生時間點早於範圍開始時間，則移至下一週
         if (firstPossibleOccurrence.isBefore(rangeStart)) {
             firstPossibleOccurrence = firstPossibleOccurrence.plusWeeks(1);
         }

        // 從第一個可能發生時間點開始，一週一週往後檢查
        LocalDateTime potentialOccurrence = firstPossibleOccurrence;
        while (!potentialOccurrence.isAfter(rangeEnd)) {
            // 如果當前檢查的時間點在範圍內 (包含邊界)
            if (!potentialOccurrence.isBefore(rangeStart) && !potentialOccurrence.isAfter(rangeEnd)) {
                return true; // 找到一次發生，返回 true
            }
            // 移至下一週
            potentialOccurrence = potentialOccurrence.plusWeeks(1);
        }

        return false; // 在整個範圍內沒有找到任何發生時間
    }

    // 實現 CourseService 介面中新加入的 getCourseDetailsIncludingCounts 方法
 	@Override
 	@Transactional(readOnly = true)
 	public CourseResponse getCourseDetailsIncludingCounts(Integer id) {
 		logger.info("Service: getCourseDetailsIncludingCounts 獲取課程 ID {} 的詳細資訊 (包含人數)", id);
 		// 呼叫 CourseDAO 中覆寫的 findById(Integer id) 方法並加上 @EntityGraph({"images", "coach"})
 		Course course = courseDAO.findById(id)
 				.orElseThrow(() -> new EntityNotFoundException("找不到課程 ID: " + id));
 		// 獲取常規報名人數和體驗預約人數
 		int registeredCount = enrollmentService.getEnrolledCount(id);
 		logger.debug("Service: 課程 ID {} 的常規已報名人數為: {}", id, registeredCount);
 		// 獲取單個課程的下一個排程的 bookedTrialCount (如果提供體驗選項)
 		Integer bookedTrialCount = 0;
 		boolean courseOffersTrial = course.getOffersTrialOption() != null && course.getOffersTrialOption();
 		if (courseOffersTrial) {
 			List<Course> singleCourseList = Collections.singletonList(course);
 			Map<Integer, Integer> countsMap = getNextOccurrenceBookedTrialCounts(singleCourseList);
 			bookedTrialCount = countsMap.getOrDefault(course.getId(), 0);
 			if (bookedTrialCount == null) {
 				bookedTrialCount = 0;
 			}
 		}
 		logger.debug("Service: 課程 ID {} 的體驗預約人數為: {}", id, bookedTrialCount);
 		// 計算額滿狀態
 		boolean isFull = isCourseFull(course.getId()); // 使用 CourseService 中的方法
 		boolean isTrialFull = isCourseTrialFull(course.getId(), bookedTrialCount); // 使用 CourseService 中的方法


 		logger.debug("Service: 課程 ID {} 額滿狀態計算: 常規={}, 體驗={}", id, isFull, isTrialFull);
 		// 映射為 CourseResponse DTO 並填充欄位
 		CourseResponse courseResponse = CourseResponse.builder()
 				.id(course.getId())
 				.name(course.getName())
 				.description(course.getDescription())
 				// 從 Course 實體中的 User coach 物件中獲取教練 ID 和名字
 				.coachId(course.getCoach() != null ? course.getCoach().getId() : null)
 				.coachName(course.getCoach() != null ? course.getCoach().getName() : null)
 				.dayOfWeek(course.getDayOfWeek())
 				.startTime(course.getStartTime())
 				.duration(course.getDuration())
 				.maxCapacity(course.getMaxCapacity())
 				.offersTrialOption(course.getOffersTrialOption())
 				.maxTrialCapacity(course.getMaxTrialCapacity())
 				.registeredCount(registeredCount)
 				.bookedTrialCount(bookedTrialCount)
 				.full(isFull)
 				.trialFull(isTrialFull)
 				// userStatus 和 userTrialBookingStatus 不在此處填充，留給 Controller
 				.userStatus(null)
 				.userTrialBookingStatus(null)
                // 將轉換好的圖片列表設置到 DTO 中
                .images(convertToImageDtoList(course.getImages()))
 				.build();
 		logger.info("Service: 課程 ID {} 的 CourseResponse DTO (含人數) 構建完成。", id);

 		return courseResponse; // 返回填充好人數和狀態的 DTO
 	}

    // 實現 CourseService 介面新加入的 isCourseFull 方法
    @Override
    @Transactional(readOnly = true)
    public boolean isCourseFull(Integer courseId) {
        // 這裡直接呼叫 EnrollmentService 中的 isCourseFull 方法
        // 因為檢查課程是否滿的核心邏輯應該在 EnrollmentService 中實現
        // 為了避免循環依賴，CourseService 依賴 EnrollmentService 是可以接受的
        return enrollmentService.isCourseFull(courseId);
    }

    // 輔助方法：檢查體驗課程是否已滿
    // 注意：這個方法在 CourseServiceImpl 內部使用，不需要暴露在 CourseService 介面中
    private boolean isCourseTrialFull(Integer courseId, Integer bookedTrialCount) {
        logger.info("檢查體驗課程 ID {} 是否已滿...", courseId);
        // 獲取課程信息以檢查 maxTrialCapacity
        Course course = courseDAO.findById(courseId)
                .orElseThrow(() -> new EntityNotFoundException("Course not found with id: " + courseId));
        boolean courseOffersTrial = course.getOffersTrialOption() != null && course.getOffersTrialOption();
        Integer maxTrialCapacity = course.getMaxTrialCapacity();
        // 檢查是否提供體驗選項，最大體驗容量是否不為 null 且大於 0，以及已預約人數是否達到或超過容量
        boolean isTrialFull = courseOffersTrial && maxTrialCapacity != null && maxTrialCapacity > 0 &&
                              bookedTrialCount >= maxTrialCapacity;
        logger.info("體驗課程 ID {} 已預約人數: {}，最大體驗容量: {}，是否已滿: {}",
                   courseId, bookedTrialCount, maxTrialCapacity, isTrialFull);
        return isTrialFull;
    }

    // 實現 CourseService 介面新加入的 hasActiveEnrollmentsForCourse 方法
    @Override
    @Transactional(readOnly = true)
    public boolean hasActiveEnrollmentsForCourse(Integer courseId) {
        // 這裡直接呼叫 EnrollmentService 中的 hasActiveEnrollmentsForCourse 方法
        // 因為檢查活躍報名記錄的核心邏輯應該在 EnrollmentService 中實現
        return enrollmentService.hasActiveEnrollmentsForCourse(courseId);
    }
}
package com.healthmanagement.dao.course;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param; // 引入 @Param
import org.springframework.stereotype.Repository;

import com.healthmanagement.model.course.Course;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

@Repository
public interface CourseDAO extends JpaRepository<Course, Integer> {
    // 用課程名稱查詢
    List<Course> findByNameContainingIgnoreCase(String name);
    // 用教練ID查詢 (可以直接使用，因為 coach_id 現在是 users 表格的 user_id)
    List<Course> findByCoachId(Integer coachId);
    // 用教練名稱查詢 (需要使用 @Query 進行關聯查詢)
    @Query("SELECT c FROM Course c JOIN c.coach u WHERE u.name LIKE %:coachName%")
    List<Course> findByCoachNameContainingIgnoreCase(@Param("coachName") String coachName); // 使用 @Param

    // 用星期幾查詢 (非分頁版本，如果不需要可以移除)
    List<Course> findByDayOfWeek(Integer dayOfWeek);
    // 根據開始日期時間範圍查詢 (如果不需要可以移除)
    List<Course> findByStartTimeBetween(LocalDateTime startTime, LocalDateTime endTime);

    // *** 現有支援分頁和篩選的方法 (可以保留或移除，取決於您的服務層如何呼叫) ***
    // 根據 offersTrialOption 查詢並支援分頁
    Page<Course> findByOffersTrialOption(Boolean offersTrialOption, Pageable pageable);
    // 根據 dayOfWeek 查詢並支援分頁
    Page<Course> findByDayOfWeek(Integer dayOfWeek, Pageable pageable); // 注意這裡與上面的非分頁版本方法名相同，但參數不同
    // 根據 offersTrialOption 和 dayOfWeek 查詢並支援分頁
    Page<Course> findByOffersTrialOptionAndDayOfWeek(Boolean offersTrialOption, Integer dayOfWeek, Pageable pageable);

    Page<Course> findByCoachId(Integer coachId, Pageable pageable);

    // *** 修改：根據 offersTrialOption, dayOfWeek, 和 isFull 查詢並支援分頁的方法 ***
    // 這個方法將用於getAllCoursesWithUserStatus，可以處理所有主要的篩選組合，包括滿額狀態
    // isFull = TRUE 表示查詢已滿的課程 (常規滿或體驗滿)
    // isFull = FALSE 表示查詢未滿/快滿的課程 (常規未滿且體驗未滿或不提供體驗)
    // isFull = NULL 表示不根據滿額狀態進行篩選
    @Query("SELECT c FROM Course c " +
           "WHERE (:offersTrialOption IS NULL OR c.offersTrialOption = :offersTrialOption) " + // Filter by offersTrialOption (if provided)
           "  AND (:dayOfWeek IS NULL OR c.dayOfWeek = :dayOfWeek) " + // Filter by dayOfWeek (if provided)
           "  AND (" + // Start fullness status filter conditions
           "       (:isFull IS NULL) " + // 如果 isFull 為 NULL，則不應用滿額狀態過濾
           "       OR (:isFull = TRUE AND (" + // 如果 isFull 為 TRUE (查詢 '已額滿')
           "            (COALESCE((SELECT COUNT(e_sub) FROM Enrollment e_sub WHERE e_sub.course.id = c.id AND e_sub.status = 'ACTIVE'), 0) >= c.maxCapacity AND c.maxCapacity IS NOT NULL AND c.maxCapacity > 0)" + // 常規課程已滿 (活躍報名 >= 最大人數)
           "            OR (c.offersTrialOption = TRUE AND c.maxTrialCapacity IS NOT NULL AND c.maxTrialCapacity > 0 AND COALESCE((SELECT COUNT(bt_sub) FROM TrialBooking bt_sub WHERE bt_sub.course.id = c.id AND bt_sub.bookingStatus = 'ACTIVE'), 0) >= c.maxTrialCapacity)" + // 體驗課程已滿 (如果提供體驗且活躍預約 >= 最大體驗人數)
           "           )" +
           "       )" +
           "       OR (:isFull = FALSE AND NOT (" + // 如果 isFull 為 FALSE (查詢 '未額滿'/'快額滿'，即非已額滿)
           "            (COALESCE((SELECT COUNT(e_sub) FROM Enrollment e_sub WHERE e_sub.course.id = c.id AND e_sub.status = 'ACTIVE'), 0) >= c.maxCapacity AND c.maxCapacity IS NOT NULL AND c.maxCapacity > 0)" + // 常規課程已滿
           "            OR (c.offersTrialOption = TRUE AND c.maxTrialCapacity IS NOT NULL AND c.maxTrialCapacity > 0 AND COALESCE((SELECT COUNT(bt_sub) FROM TrialBooking bt_sub WHERE bt_sub.course.id = c.id AND bt_sub.bookingStatus = 'ACTIVE'), 0) >= c.maxTrialCapacity)" + // 體驗課程已滿 (如果提供體驗)
           "           )" + // 這裡使用 NOT 包裹 '已額滿' 的條件來實現 '未額滿'/'快額滿' 的邏輯
           "       )" +
           ")") // 結束滿額狀態過濾條件
    Page<Course> findCoursesWithFilters(
        Pageable pageable,
        @Param("offersTrialOption") Boolean offersTrialOption, // Filter for trial option, 使用 @Param
        @Param("dayOfWeek") Integer dayOfWeek,         // Filter for day of week, 使用 @Param
        @Param("isFull") Boolean isFull             // Filter for fullness (TRUE for full, FALSE for not full, NULL for no filter), 使用 @Param
    );

    // 請注意：
    // 1. 上述 JPQL 假設您的 Course 實體與 Enrollment 和 TrialBooking 實體有正確的關係映射。
    // 2. 'ACTIVE' 狀態需要與您的 Enrollment 和 TrialBooking 實體中的實際活躍狀態值匹配。
    // 3. JPQL 使用子查詢來計算報名和預約人數，並根據 maxCapacity 和 maxTrialCapacity 來判斷是否額滿。
    // 4. COALESCE(..., 0) 用於處理沒有報名或預約記錄的情況，將 COUNT 結果視為 0。
    // 5. 增加了對 maxCapacity 和 maxTrialCapacity 非空且大於 0 的檢查，避免潛在問題。
    // 6. JPQL 語法可能需要根據您具體的 JPA 提供者 (Hibernate, EclipseLink 等) 和資料庫微調。
    // 7. 在 Service 層 (例如 EnrollmentServiceImpl 的 getAllCoursesWithUserStatus 方法) 中，您需要將接收到的 String fullnessStatus ("full", "notFull", null) 轉換為 Boolean 類型的 isFull (String "full" -> Boolean.TRUE, String "notFull" -> Boolean.FALSE, null -> null)，然後將這個 Boolean 值傳給這個 findCoursesWithFilters DAO 方法。

    // 其他您現有的方法...
}
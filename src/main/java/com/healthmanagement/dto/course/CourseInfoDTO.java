package com.healthmanagement.dto.course;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalTime;
import java.util.List; // 新增: 引入 List

// 新增: 引入 CourseImageDto
// 請確保 com.healthmanagement.dto.course.CourseImageDto 檔案存在且定義正確
import com.healthmanagement.dto.course.CourseImageDTO;


@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CourseInfoDTO {

	private Integer id;
	private String name;
	private String description; // 課程描述
	private Integer dayOfWeek; // 課程星期幾
	private LocalTime startTime; // 課程開始時間
	private Integer duration; // 課程時長
	private Integer maxCapacity; // 最大容納人數

 	// **新增 coachId 和 coachName 字段**
 	private Integer coachId;
 	private String coachName;

 	// **新增的常規報名匯總資訊**
 	private Integer registeredCount; // 常規已報名人數
	private Integer waitlistCount;// 常規候補人數

	// **新增的常規報名使用者狀態**
	private String userStatus;// 當前查詢使用者的常規報名狀態 ('已報名', '候補中', '未報名', 等)
	private Integer userEnrollmentId; // 如果使用者有常規報名記錄，其記錄 ID

	// **新增的體驗預約相關資訊**
	private Boolean offersTrialOption; // 是否提供體驗預約選項
	private Integer maxTrialCapacity; // 體驗課最大人數 (如果 offersTrialOption 為 true)
	private Integer bookedTrialCount; // 已預約體驗人數 (針對下一個排程或總數，根據 Service 邏輯)

	// **新增的體驗預約使用者狀態**
	private String userTrialBookingStatus; // 當前使用者在體驗預約方面的狀態 ('已預約', '未預約')
	private Integer userTrialBookingId; // 如果使用者有體驗預約記錄，其記錄 ID

	// **在 CourseInfoDTO 中新增 isFull 屬性**
 	private boolean isFull;

 	private boolean isTrialFull;

    // ======= 新增圖片列表欄位 =======
    // 添加一個 CourseImageDto 的列表來存放課程圖片資訊
    private List<CourseImageDTO> images;
    // ======= 圖片列表欄位新增結束 =======

}
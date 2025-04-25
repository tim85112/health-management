package com.healthmanagement.dto.course;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalTime;
// 您可能還需要引入 Course Entity 中的其他欄位類型，例如 DayOfWeek
// import java.time.DayOfWeek;

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
    // private Integer coachId; // 如果需要教練 ID
    // private String coachName; // 如果需要教練姓名

    // **新增的常規報名匯總資訊**
    private Integer registeredCount; // 常規已報名人數
    private Integer waitlistCount;    // 常規候補人數

    // **新增的常規報名使用者狀態**
    private String userStatus;        // 當前查詢使用者的常規報名狀態 ('已報名', '候補中', '未報名', 等)
    private Integer userEnrollmentId; // 如果使用者有常規報名記錄，其記錄 ID

    // **新增的體驗預約相關資訊**
    private Boolean offersTrialOption; // 是否提供體驗預約選項
    private Integer maxTrialCapacity; // 體驗課最大人數 (如果 offersTrialOption 為 true)
    private Integer bookedTrialCount; // 已預約體驗人數 (針對下一個排程或總數，根據 Service 邏輯)

    // **新增的體驗預約使用者狀態**
    private String userTrialBookingStatus; // 當前使用者在體驗預約方面的狀態 ('已預約', '未預約')
    private Integer userTrialBookingId; // 如果使用者有體驗預約記錄，其記錄 ID

    // 您可以根據需要在這裡加入更多欄位
}
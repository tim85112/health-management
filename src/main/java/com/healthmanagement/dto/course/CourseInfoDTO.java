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
    // 可以包含更多 Course Entity 的欄位
    private Integer dayOfWeek; // 課程星期幾
    private LocalTime startTime; // 課程開始時間
    private Integer duration; // 課程時長
    private Integer maxCapacity; // 最大容納人數
    // private Integer coachId; // 如果需要教練 ID

    // **新增的匯總資訊**
    private Integer registeredCount; // 已報名人數
    private Integer waitlistCount;   // 候補人數

    // **新增的使用者狀態**
    private String userStatus;       // 當前查詢使用者的報名狀態 ('已報名', '候補中', '未報名')

    // 您可以根據需要在這裡加入更多欄位
}
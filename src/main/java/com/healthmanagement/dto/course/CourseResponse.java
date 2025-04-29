package com.healthmanagement.dto.course;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import java.time.LocalTime;

@Data
@Builder
@AllArgsConstructor
public class CourseResponse {
	private Integer id;
	private String name;
	private String description;
	private Integer coachId;
	private String coachName;
	private Integer dayOfWeek;
	private LocalTime startTime;
	private Integer duration;
	private Integer maxCapacity;
    private Boolean offersTrialOption;
    private Integer maxTrialCapacity;
    private Integer bookedTrialCount; // 已預約體驗人數 (Service 計算後設定)

    private Integer registeredCount; // 新增：已報名人數
    private Boolean full; // 新增：常規課程額滿狀態
    private Boolean trialFull;
    // 注意：如果你的 CourseResponse 包含 User coach 對象而不是 coachId/coachName，
    // 這裡的字段結構會不同。但根據你之前提供的 CourseServiceImpl 和 DTO，目前是 coachId/coachName。
}
package com.healthmanagement.dto.course;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import java.time.LocalTime;
import java.util.List;


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
    private Integer bookedTrialCount;

    private Integer registeredCount;
    private Boolean full;
    private Boolean trialFull;

    // 表示當前使用者的狀態
    private String userStatus; // 例如: "已報名", "未報名", "已完成", "已取消", "未登入"
    private String userTrialBookingStatus; // 例如: "已預約", "未預約", "已取消", "未提供", "未登入"
    
    // 用於存放課程圖片的列表
    private List<CourseImageDTO> images;
}
package com.healthmanagement.dto.course;

import lombok.Data;
import java.time.LocalTime;

// 導入驗證註解
import jakarta.validation.constraints.Max;     // <--- 改為 jakarta
import jakarta.validation.constraints.Min;     // <--- 改為 jakarta
import jakarta.validation.constraints.NotBlank; // <--- 改為 jakarta
import jakarta.validation.constraints.NotNull; // <--- 改為 jakarta

@Data
public class CourseRequest {

    // 課程名稱：必填，非空字串
    @NotBlank(message = "課程名稱不能為空")
	private String name;

    // 課程內容：您可以決定是否必填，如果必填，也用 @NotBlank
    // @NotBlank(message = "課程內容不能為空") // 如果課程內容是必填
	private String description;

    // 星期幾：必填，不能為 null，值必須在 0 到 6 之間
    @NotNull(message = "星期幾不能為空")
    @Min(value = 0, message = "星期幾必須在 0 到 6 之間")
    @Max(value = 6, message = "星期幾必須在 0 到 6 之間")
	private Integer dayOfWeek;

    // 開始時間：必填，不能為 null
    @NotNull(message = "開始時間不能為空")
	private LocalTime startTime;

    // 時長：必填，不能為 null，值必須大於 0
    @NotNull(message = "時長不能為空")
    @Min(value = 1, message = "時長必須大於 0")
	private Integer duration;

    // 最大人數：必填，不能為 null，值必須大於 0
    @NotNull(message = "最大人數不能為空")
    @Min(value = 1, message = "最大人數必須大於 0")
	private Integer maxCapacity;

    // 教練編號：必填，不能為 null，值必須大於 0 (假設教練 ID 從 1 開始)
    @NotNull(message = "教練編號不能為空")
	private Integer coachId;

    // 是否為體驗課：必填，不能為 null (如果是 Boolean 包裝類)
    @NotNull(message = "是否為體驗課不能為空")
	private Boolean offersTrialOption;

    // 最大體驗人數：如果 offersTrialOption 為 true 時必填且大於 0。
    // 簡單處理：如果提供了值，必須大於 0。更複雜的條件驗證需要 @ValidateIf 或驗證群組。
    // 如果只要求值大於 0（當提供了值時），可以使用 @Min
    // @NotNull(message = "最大體驗人數不能為空") // 只有在 offersTrialOption 為 true 時才需要 NotNull
    @Min(value = 1, message = "最大體驗人數必須大於 0")
	private Integer maxTrialCapacity; // 在 offersTrialOption 為 true 時有效
}
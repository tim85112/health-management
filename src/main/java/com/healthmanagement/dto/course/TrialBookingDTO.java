package com.healthmanagement.dto.course;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TrialBookingDTO {

    private Integer id;
    private Integer userId;
    private Integer courseId;
    private String bookingName;
    private String bookingEmail; // 保持使用 bookingEmail

    private String bookingPhone;
    private LocalDate bookingDate; // 保留，用於後端邏輯或數據來源
    private LocalTime startTime; // 保留，用於後端邏輯或數據來源
    private String bookingStatus;
    private LocalDateTime bookedAt; // 保留，用於顯示預約建立時間

    private String courseName;
    private String userName;
    private String coachName;

    // *** 新增: 用來表示課程排定時間的欄位，供前端使用 prop="bookingTime" ***
    // 建議使用 LocalDateTime 類型
    private LocalDateTime bookingTime;
    
    

    // 注意：您需要在將 Entity 映射到 DTO 的邏輯中，
    // 將 bookingDate 和 startTime 組合起來，設定到這個 bookingTime 屬性裡。
    // 同時也要確保 bookedAt 的值有被正確設定。
}
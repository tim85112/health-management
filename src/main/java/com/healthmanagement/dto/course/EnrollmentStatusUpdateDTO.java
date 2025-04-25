package com.healthmanagement.dto.course;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import jakarta.validation.constraints.NotBlank; // 引入用於驗證

@Data
@NoArgsConstructor
@AllArgsConstructor
public class EnrollmentStatusUpdateDTO {

    @NotBlank(message = "狀態不能為空") // 確保更新時必須提供狀態
    private String status;

    // 如果需要更新其他欄位，可以在這裡添加
    // private String notes;
    // private Integer courseId; // 例如，如果允許轉課，但這通常更複雜
}
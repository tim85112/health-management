package com.healthmanagement.dto.course;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import jakarta.validation.constraints.NotBlank;

/**
 * 用於更新預約狀態的數據傳輸對象。
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class BookingStatusUpdateDTO {

    /**
     * The target status for the booking.
     * 要更新的目標狀態，例如 "已取消", "已完成" 等。
     */
    @NotBlank(message = "新狀態不能為空") // 確保這個欄位不為 null 且不為空白字串
    private String newStatus;

    // 如果需要，您也可以在這裡添加其他字段，例如：
    // private String reason; // 更改狀態的原因
    // private Integer changedByUserId; // 由誰更改 (如果後端自己獲取則不需要放在這裡)
}
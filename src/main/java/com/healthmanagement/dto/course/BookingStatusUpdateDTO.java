package com.healthmanagement.dto.course;


import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BookingStatusUpdateDTO {

    private String status;

    // 添加其他欄位如果需要更新狀態 (例如，備註，原因)
    // private String notes;
}
package com.healthmanagement.dto.course;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TrialBookingRequestDTO {

    @NotNull(message = "課程 ID 不能為空")
    private Integer courseId;

    @NotBlank(message = "報名姓名不能為空")
    private String bookingName;

    @NotBlank(message = "電話號碼不能為空")
    @Pattern(regexp = "^[0-9]{8,15}$", message = "電話號碼格式不正確") // 示例驗證
    private String bookingPhone;

    @NotNull(message = "預約日期不能為空")
    private LocalDate bookingDate;
}
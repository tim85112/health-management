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
    private String bookingPhone;
    private LocalDate bookingDate;
    private LocalTime startTime;
    private String bookingStatus;
    private LocalDateTime bookedAt;
    private String courseName;
    private String userName;
    private String coachName;
}

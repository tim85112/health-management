package com.healthmanagement.model.course;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDate;
import java.time.LocalDateTime;

import com.healthmanagement.model.member.User;
import com.fasterxml.jackson.annotation.JsonBackReference;

@Data
@Entity
@Table(name = "trial_booking")
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TrialBooking {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false)
    @JsonBackReference("user-trial-bookings")
    private User user;

    @ManyToOne
    @JoinColumn(name = "course_id", nullable = false)
    @JsonBackReference
    private Course course;

    @Column(name = "booking_name", nullable = false, length = 255)
    private String bookingName;

    @Column(name = "booking_phone", nullable = false, length = 20)
    private String bookingPhone;

    @Column(name = "booking_date")
    private LocalDate bookingDate;

    @Column(name = "booking_status", nullable = false, length = 50)
    @Builder.Default
    private String bookingStatus = "已預約";

    @Column(name = "booked_at", nullable = false)
    @Builder.Default
    private LocalDateTime bookedAt = LocalDateTime.now();
}
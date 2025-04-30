package com.healthmanagement.model.course;

import com.fasterxml.jackson.annotation.JsonBackReference;
import com.healthmanagement.model.member.User; // 確保引入 User 實體
import jakarta.persistence.*; // JPA 註解
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDate;
import java.time.LocalDateTime; // 假設 booked_at 是 LocalDateTime

import org.hibernate.annotations.CreationTimestamp;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity // 標記為 JPA 實體
@Table(name = "trial_booking") // 對應的資料表名稱
public class TrialBooking {

    @Id // 主鍵
    @GeneratedValue(strategy = GenerationType.IDENTITY) // 主鍵生成策略，例如 IDENTITY
    @Column(name = "id")
    private Integer id;

    // 與 User 實體的 Many-to-One 關聯 (多個預約對應一個用戶)
    // 外鍵是 trial_booking 表的 user_id 欄位指向 users 表的 id
    @ManyToOne(fetch = FetchType.LAZY) // LAZY Fetch 避免一次性載入 User 完整資料
    @JoinColumn(name = "user_id", nullable = true) // 對應資料庫的外鍵欄位，允許為 NULL
     @JsonBackReference("user-trial-bookings") // 如果需要處理雙向關聯的 JSON 序列化問題
    private User user; // 如果 user_id 為 NULL，這個 User 物件會是 null

    // 與 Course 實體的 Many-to-One 關聯 (多個預約對應一個課程)
    @ManyToOne(fetch = FetchType.LAZY) // LAZY Fetch 避免一次性載入 Course 完整資料
    @JoinColumn(name = "course_id", nullable = false) // 對應資料庫的外鍵欄位，不允許為 NULL
     @JsonBackReference("course-trial-bookings") // 如果需要處理雙向關聯的 JSON 序列化問題
    private Course course; // 預約的課程

    @Column(name = "booking_name", nullable = false, length = 100) // 報名姓名
    private String bookingName;

    // *** MODIFICATION: 將 email 屬性名稱和對應的欄位名稱改為 booking_email ***
    @Column(name = "booking_email", nullable = false, length = 255) // 與 DB 欄位名稱 booking_email 符
    private String bookingEmail; // 屬性名稱建議使用駝峰式

    @Column(name = "booking_phone", nullable = false, length = 15) // 報名電話
    private String bookingPhone;

    @Column(name = "booking_date", nullable = false) // 預約日期
    private LocalDate bookingDate;

    @Column(name = "booking_status", nullable = false, length = 50) // 預約狀態
    private String bookingStatus;

    @Column(name = "booked_at") // 預約時間戳 (資料庫自動生成)
    @CreationTimestamp
    private LocalDateTime bookedAt;

    // 如果您在 Course 實體中有 trial_bookings 列表，這裡的 @JsonBackReference 是對應的
    // 請確保您的 User 和 Course 實體中的關聯映射是正確的
}

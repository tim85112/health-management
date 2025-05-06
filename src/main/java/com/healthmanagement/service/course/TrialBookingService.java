package com.healthmanagement.service.course;

import com.healthmanagement.dto.course.TrialBookingRequestDTO;
import com.healthmanagement.model.member.User;
import com.healthmanagement.dto.course.TrialBookingDTO;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface TrialBookingService {

	// 為使用者或訪客預約體驗課程
	TrialBookingDTO bookTrialCourse(User user, String contactEmail, TrialBookingRequestDTO bookingRequestDTO);
	// 獲取特定使用者的所有試上預約記錄
    List<TrialBookingDTO> getTrialBookingsByUserId(Integer userId);
	// 獲取特定課程的所有試上預約記錄
    List<TrialBookingDTO> getTrialBookingsByCourseId(Integer courseId);
	// 取消試上預約
    void cancelTrialBooking(Integer bookingId);
	// 獲取特定試上預約記錄的詳細資訊
    TrialBookingDTO getTrialBookingDetails(Integer bookingId);
	// 更新試上預約記錄的狀態
    TrialBookingDTO updateBookingStatus(Integer bookingId, String newStatus);
    // 獲取所有體驗預約紀錄 (支援分頁和篩選)
    Page<TrialBookingDTO> getAllTrialBookings(Pageable pageable, String bookingStatus, Integer courseId, Integer userId);
    // 根據會員名稱查詢體驗預約記錄的方法簽名 ***
    List<TrialBookingDTO> searchTrialBookingsByUserName(String userName);
}
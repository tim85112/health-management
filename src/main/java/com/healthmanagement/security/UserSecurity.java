package com.healthmanagement.security;

import com.healthmanagement.dao.course.EnrollmentDAO;
import com.healthmanagement.dao.course.TrialBookingDAO;
import com.healthmanagement.model.course.Enrollment;
import com.healthmanagement.model.course.TrialBooking;
import com.healthmanagement.service.member.UserService;

import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

@Component("userSecurity")
public class UserSecurity {

    @Autowired
    private UserService userService;

    @Autowired
    private EnrollmentDAO enrollmentDAO;

    @Autowired
    private TrialBookingDAO trialBookingDAO;

    /**
     * 檢查當前登錄用戶是否為指定的用戶ID
     * 
     * @param userId 用戶ID
     * @return 如果當前用戶是指定ID的用戶或是管理員，則返回true
     */
    public boolean isCurrentUser(Integer userId) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            return false;
        }

        // 檢查是否為管理員
        if (authentication.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("admin"))) {
            return true;
        }

        String currentUserEmail = authentication.getName();

        try {
            // 從數據庫獲取當前用戶信息
            return userService.findByEmail(currentUserEmail)
                    .map(user -> userId.equals(user.getId()))
                    .orElse(false);
        } catch (Exception e) {
            // 忽略解析錯誤，返回false
            return false;
        }
    }

    // 檢查當前登錄用戶是否為指定報名 ID 的擁有者
    public boolean isCurrentUserByEnrollmentId(Integer enrollmentId) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            return false;
        }
        // 檢查是否為管理員
        if (authentication.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("admin"))) {
            return true;
        }
        String currentUserEmail = authentication.getName();
        try {
            Optional<Enrollment> enrollmentOptional = enrollmentDAO.findById(enrollmentId);
            if (enrollmentOptional.isPresent()) {
                Enrollment enrollment = enrollmentOptional.get();
                return userService.findByEmail(currentUserEmail)
                        .map(user -> enrollment.getUser().getId().equals(user.getId()))
                        .orElse(false);
            }
            return false; // 如果找不到該報名記錄，則返回 false
        } catch (Exception e) {
            // 忽略解析錯誤，返回 false
            return false;
        }
    }

    // 檢查當前登錄用戶是否為指定體驗預約 ID 的擁有者
    public boolean isCurrentUserByTrialBookingId(Integer bookingId) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            return false; // 未認證用戶一律無權限
        }
        // 檢查是否為管理員 (管理員擁有所有權限)
        if (authentication.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("admin"))) {
            return true;
        }
        // 檢查是否為教練 (如果教練擁有對所有體驗預約的查看或管理權限，可以取消註解)
        if (authentication.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("coach"))) {
            return true;
        }
        String currentUserEmail = authentication.getName(); // 獲取當前登入用戶的郵箱
        try {
            // 查找指定的體驗預約記錄
            Optional<TrialBooking> bookingOptional = trialBookingDAO.findById(bookingId);
            if (bookingOptional.isPresent()) {
                TrialBooking trialBooking = bookingOptional.get();
                // 查找當前登入使用者，並比較其 ID 是否與體驗預約記錄的使用者 ID 相同
                return userService.findByEmail(currentUserEmail)
                        .map(user -> trialBooking.getUser() != null
                                && trialBooking.getUser().getId().equals(user.getId())) // 確保 trialBooking.getUser() 不為
                                                                                        // null
                        .orElse(false); // 如果找不到當前使用者，則返回 false
            }
            return false; // 如果找不到該體驗預約記錄，則返回 false
        } catch (Exception e) {
            // 忽略查找或比較過程中可能發生的錯誤，返回 false
            System.err.println("Error during isCurrentUserByTrialBookingId check for booking ID " + bookingId + ": "
                    + e.getMessage()); // 記錄錯誤
            return false;
        }
    }
}
package com.healthmanagement.security;

import com.healthmanagement.service.member.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import com.healthmanagement.dao.course.EnrollmentDAO;
import com.healthmanagement.dao.course.TrialBookingDAO;
import com.healthmanagement.model.course.Enrollment;
import com.healthmanagement.model.course.TrialBooking;
import com.healthmanagement.model.member.User;
import org.springframework.security.oauth2.core.user.OAuth2User;


import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


@Component("userSecurity")
public class UserSecurity {

    @Autowired
    private UserService userService;

    @Autowired
    private EnrollmentDAO enrollmentDAO;
    @Autowired
    private TrialBookingDAO trialBookingDAO;

    private static final Logger logger = LoggerFactory.getLogger(UserSecurity.class);

    @Value("${app.guest-user-id}")
    private Integer guestUserId;

	// 檢查當前登錄用戶是否為指定的用戶ID。
    public boolean isCurrentUser(Integer targetUserId) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        // 處理未認證用戶和訪客資源訪問
        if (authentication == null || !authentication.isAuthenticated()) {
            if (targetUserId != null && targetUserId.equals(guestUserId)) {
                logger.debug("UserSecurity.isCurrentUser: 匿名用戶嘗試訪問訪客資源，目標ID: {}", targetUserId);
                return true; // 允許匿名用戶訪問訪客資源
            }
            logger.debug("UserSecurity.isCurrentUser: 未認證用戶或非訪客資源訪問。");
            return false; // 未認證用戶且非訪客資源
        }
        // 檢查是否為管理員 (管理員擁有所有權限)
        if (authentication.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("admin"))) {
            logger.debug("UserSecurity.isCurrentUser: 當前用戶是管理員，允許訪問。");
            return true;
        }
        // 對於已認證的非管理員用戶，嘗試獲取當前用戶 ID
        Integer currentUserId = getCurrentAuthenticatedUserId(authentication);

        if (currentUserId == null) {
             logger.warn("UserSecurity.isCurrentUser: 無法獲取當前已認證用戶的 ID。");
             return false;
        }
        logger.debug("UserSecurity.isCurrentUser: 獲取到當前用戶 ID: {}，目標 ID: {}", currentUserId, targetUserId);
        return targetUserId != null && targetUserId.equals(currentUserId);
    }

	// 檢查當前登錄用戶是否為指定報名 ID 的擁有者。
    public boolean isCurrentUserByEnrollmentId(Integer enrollmentId) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        // 處理未認證用戶 (訪客用戶通常沒有報名記錄)
        if (authentication == null || !authentication.isAuthenticated()) {
             logger.debug("UserSecurity.isCurrentUserByEnrollmentId: 未認證用戶，不允許訪問報名記錄。");
             return false;
        }
        // 檢查是否為管理員 (管理員擁有所有權限)
        if (authentication.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("admin"))) {
            logger.debug("UserSecurity.isCurrentUserByEnrollmentId: 當前用戶是管理員，允許訪問。");
            return true;
        }
        // 對於已認證的非管理員用戶，嘗試獲取當前用戶 ID
        Integer currentUserId = getCurrentAuthenticatedUserId(authentication);

        if (currentUserId == null) {
             logger.warn("UserSecurity.isCurrentUserByEnrollmentId: 無法獲取當前已認證用戶的 ID。");
             return false;
        }
         logger.debug("UserSecurity.isCurrentUserByEnrollmentId: 獲取到當前用戶 ID: {}", currentUserId);

        try {
            Optional<Enrollment> enrollmentOptional = enrollmentDAO.findById(enrollmentId);
            if (enrollmentOptional.isPresent()) {
                Enrollment enrollment = enrollmentOptional.get();
                // 比較報名記錄的使用者 ID 與當前用戶的 ID
                boolean isOwner = enrollment.getUser() != null && enrollment.getUser().getId().equals(currentUserId);
                logger.debug("UserSecurity.isCurrentUserByEnrollmentId: 報名 ID {} 的擁有者 ID: {}，當前用戶 ID: {}，是否匹配: {}", enrollmentId, enrollment.getUser() != null ? enrollment.getUser().getId() : "null", currentUserId, isOwner);
                return isOwner;
            }
             logger.debug("UserSecurity.isCurrentUserByEnrollmentId: 未找到報名 ID {} 的記錄。", enrollmentId);
            // 如果找不到該報名記錄，則返回 false
            return false;
        } catch (Exception e) {
            logger.error("UserSecurity.isCurrentUserByEnrollmentId: 檢查報名 ID {} 所有權時發生錯誤。", enrollmentId, e);
            return false;
        }
    }


	// 檢查當前登錄用戶是否為指定體驗預約 ID 的擁有者。
    public boolean isTrialBookingOwner(Integer bookingId) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        // 處理未認證用戶 (訪客用戶通常沒有體驗預約記錄)
        if (authentication == null || !authentication.isAuthenticated()) {
             logger.debug("UserSecurity.isTrialBookingOwner: 未認證用戶，不允許訪問體驗預約記錄。");
            return false;
        }

        // 檢查是否為管理員 (管理員擁有所有權限)
        if (authentication.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("admin"))) {
            logger.debug("UserSecurity.isTrialBookingOwner: 當前用戶是管理員，允許訪問。");
            return true;
        }

        // 對於已認證的非管理員用戶，嘗試獲取當前用戶 ID
        Integer currentUserId = getCurrentAuthenticatedUserId(authentication);

        if (currentUserId == null) {
             logger.warn("UserSecurity.isTrialBookingOwner: 無法獲取當前已認證用戶的 ID。");
             return false;
        }
         logger.debug("UserSecurity.isTrialBookingOwner: 獲取到當前用戶 ID: {}", currentUserId);


        try {
             Optional<TrialBooking> bookingOptional = trialBookingDAO.findById(bookingId);
            if (bookingOptional.isPresent()) {
                TrialBooking trialBooking = bookingOptional.get();
                // 比較體驗預約記錄的使用者 ID 與當前用戶的 ID
                boolean isOwner = trialBooking.getUser() != null && trialBooking.getUser().getId().equals(currentUserId);
                 logger.debug("UserSecurity.isTrialBookingOwner: 預約 ID {} 的擁有者 ID: {}，當前用戶 ID: {}，是否匹配: {}", bookingId, trialBooking.getUser() != null ? trialBooking.getUser().getId() : "null", currentUserId, isOwner);
                return isOwner;
            }
             logger.debug("UserSecurity.isTrialBookingOwner: 未找到體驗預約 ID {} 的記錄。", bookingId);
            // 如果找不到該體驗預約記錄，則返回 false
            return false;
        } catch (Exception e) {
            logger.error("UserSecurity.isTrialBookingOwner: 檢查預約 ID {} 所有權時發生錯誤。", bookingId, e);
            return false;
        }
    }

    // 輔助方法，用於從 Authentication 物件中獲取當前認證用戶的 ID。
    private Integer getCurrentAuthenticatedUserId(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return null; // 未認證用戶
        }

        Object principal = authentication.getPrincipal();

        // 優先檢查 Principal 是否為 Integer
        if (principal instanceof Integer) {
            logger.debug("getCurrentAuthenticatedUserId: Principal 是 Integer。");
            return (Integer) principal;
        }
        // 檢查 Principal 是否為 OAuth2User，並從 attributes 中獲取 ID
        else if (principal instanceof OAuth2User) {
            logger.debug("getCurrentAuthenticatedUserId: Principal 是 OAuth2User。");
            OAuth2User oAuth2User = (OAuth2User) principal;
            Object userIdAttribute = oAuth2User.getAttributes().get("id");
            if (userIdAttribute instanceof Integer) {
                return (Integer) userIdAttribute;
            } else {
                 logger.warn("getCurrentAuthenticatedUserId: OAuth2User 的 'id' 屬性不是 Integer 型別，實際型別: {}", userIdAttribute != null ? userIdAttribute.getClass().getName() : "null");
                 return null; // 'id' 屬性型別不正確
            }
        }
        // 如果 Principal 不是 Integer 或 OAuth2User，嘗試透過 email 查找
        else {
            logger.debug("getCurrentAuthenticatedUserId: Principal 類型非 Integer 或 OAuth2User，嘗試透過 authentication.getName() 查找。實際類型: {}", principal != null ? principal.getClass().getName() : "null");
            String currentUserEmail = authentication.getName();
            if (currentUserEmail == null) {
                logger.warn("getCurrentAuthenticatedUserId: authentication.getName() 返回 null。");
                return null;
            }
            try {
                Optional<User> currentUserOptional = userService.findByEmail(currentUserEmail);
                if (currentUserOptional.isPresent()) {
                    return currentUserOptional.get().getId();
                } else {
                    logger.warn("getCurrentAuthenticatedUserId: 透過 email {} 未找到對應的用戶。", currentUserEmail);
                    return null; // 找不到對應的用戶
                }
            } catch (Exception e) {
                logger.error("getCurrentAuthenticatedUserId: 透過 email 查找用戶時發生錯誤。", e);
                return null; // 查找錯誤
            }
        }
    }
}

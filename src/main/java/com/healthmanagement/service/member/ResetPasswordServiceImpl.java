package com.healthmanagement.service.member;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.healthmanagement.dao.member.ResetTokenRepository;
import com.healthmanagement.model.member.ResetToken;
import com.healthmanagement.model.member.User;

import jakarta.mail.Message;
import jakarta.mail.MessagingException;
import jakarta.mail.Session;
import jakarta.mail.Transport;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;
import lombok.extern.slf4j.Slf4j;

/**
 * 重設密碼服務實現類
 */
@Service
@Slf4j
public class ResetPasswordServiceImpl implements ResetPasswordService {

    private final UserService userService;
    private final ResetTokenRepository resetTokenRepository;
    private final Session mailSession;
    private final PasswordEncoder passwordEncoder;

    @Value("${spring.mail.username}")
    private String fromEmail;

    @Value("${app.frontend.url:http://localhost:5173}")
    private String frontendUrl;

    @Autowired
    public ResetPasswordServiceImpl(
            UserService userService,
            ResetTokenRepository resetTokenRepository,
            Session mailSession,
            PasswordEncoder passwordEncoder) {
        this.userService = userService;
        this.resetTokenRepository = resetTokenRepository;
        this.mailSession = mailSession;
        this.passwordEncoder = passwordEncoder;
    }

    /**
     * 發送重設密碼郵件
     * 
     * @param email 用戶電子郵件
     * @return 是否發送成功
     */
    @Override
    @Transactional
    public boolean sendResetPasswordEmail(String email) {
        // 檢查用戶是否存在
        Optional<User> userOpt = userService.findByEmail(email);
        if (userOpt.isEmpty()) {
            log.warn("找不到用戶 Email: {}", email);
            return false;
        }

        User user = userOpt.get();

        // 檢查是否已存在令牌，若有則刪除
        Optional<ResetToken> existingToken = resetTokenRepository.findByUser(user);
        existingToken.ifPresent(resetTokenRepository::delete);

        // 創建新的重設密碼令牌
        String token = UUID.randomUUID().toString();
        ResetToken resetToken = ResetToken.builder()
                .user(user)
                .token(token)
                .expiresAt(LocalDateTime.now().plusMinutes(30))
                .build();

        // 保存令牌並立即從數據庫獲取保存後的令牌以確保一致性
        resetToken = resetTokenRepository.save(resetToken);
        String savedToken = resetToken.getToken();

        // 發送包含重設鏈接的郵件，使用保存後的令牌
        String resetUrl = frontendUrl + "/reset-password?token=" + savedToken;

        // 同時打印重設令牌，方便測試
        log.info("用戶ID: {}, 重設密碼令牌: {}", user.getId(), savedToken);

        try {
            sendResetEmail(user.getEmail(), user.getName(), resetUrl);
            log.info("重設密碼郵件已發送到: {}", email);
            return true;
        } catch (MessagingException e) {
            log.error("發送重設密碼郵件失敗: {}", e.getMessage());
            return false;
        }
    }

    /**
     * 通過重設令牌更改密碼
     * 
     * @param token       重設密碼令牌
     * @param newPassword 新密碼
     * @return 是否重設成功
     */
    @Override
    @Transactional
    public boolean resetPassword(String token, String newPassword) {
        if (token == null || token.trim().isEmpty()) {
            log.warn("重設密碼令牌為空");
            return false;
        }

        log.debug("嘗試使用令牌重設密碼: {}", token);

        // 查找令牌
        Optional<ResetToken> resetTokenOpt = resetTokenRepository.findByToken(token);
        if (resetTokenOpt.isEmpty()) {
            log.warn("無效的重設密碼令牌: {}", token);
            return false;
        }

        ResetToken resetToken = resetTokenOpt.get();
        log.debug("找到重設密碼令牌: {}, 用戶ID: {}, 過期時間: {}",
                resetToken.getToken(), resetToken.getUser().getId(), resetToken.getExpiresAt());

        // 檢查令牌是否過期
        if (resetToken.isExpired()) {
            log.warn("重設密碼令牌已過期: {}, 過期時間: {}", token, resetToken.getExpiresAt());
            resetTokenRepository.delete(resetToken);
            return false;
        }

        // 更新用戶密碼 - 修正：直接設置未加密的密碼，讓UserService處理加密
        User user = resetToken.getUser();
        // 不在這裡加密，直接傳遞原始密碼
        user.setPasswordHash(newPassword);
        userService.updateUser(user.getId(), user);

        // 刪除令牌
        resetTokenRepository.delete(resetToken);
        log.info("密碼已重設，用戶ID: {}, 郵箱: {}", user.getId(), user.getEmail());

        return true;
    }

    /**
     * 驗證重設密碼令牌是否有效
     * 
     * @param token 重設密碼令牌
     * @return 令牌是否有效
     */
    @Override
    @Transactional(readOnly = true)
    public boolean validateResetToken(String token) {
        if (token == null || token.trim().isEmpty()) {
            log.warn("重設密碼令牌為空");
            return false;
        }

        log.debug("嘗試驗證重設密碼令牌: {}", token);

        // 查找令牌
        Optional<ResetToken> resetTokenOpt = resetTokenRepository.findByToken(token);
        if (resetTokenOpt.isEmpty()) {
            log.warn("無效的重設密碼令牌: {}", token);
            return false;
        }

        ResetToken resetToken = resetTokenOpt.get();
        log.debug("找到重設密碼令牌: {}, 用戶ID: {}, 過期時間: {}",
                resetToken.getToken(), resetToken.getUser().getId(), resetToken.getExpiresAt());

        // 檢查令牌是否過期
        if (resetToken.isExpired()) {
            log.warn("重設密碼令牌已過期: {}, 過期時間: {}", token, resetToken.getExpiresAt());
            return false;
        }

        log.info("重設密碼令牌有效, 用戶ID: {}, 郵箱: {}", resetToken.getUser().getId(), resetToken.getUser().getEmail());
        return true;
    }

    /**
     * 發送重設密碼郵件
     * 
     * @param to       收件人郵箱
     * @param name     收件人姓名
     * @param resetUrl 重設密碼鏈接
     * @throws MessagingException 郵件發送異常
     */
    private void sendResetEmail(String to, String name, String resetUrl) throws MessagingException {
        MimeMessage message = new MimeMessage(mailSession);
        message.setFrom(new InternetAddress(fromEmail));
        message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(to));
        message.setSubject("健康管理系統 - 重設密碼");

        String emailContent = "<div style='font-family: Arial, sans-serif; max-width: 600px; margin: 0 auto; padding: 20px; border: 1px solid #e0e0e0; border-radius: 5px;'>"
                +
                "<h2 style='color: #333;'>健康管理系統 - 重設密碼</h2>" +
                "<p>親愛的 " + name + "，</p>" +
                "<p>我們收到了重設您密碼的請求。請點擊下方鏈接來重設您的密碼：</p>" +
                "<p><a href='" + resetUrl
                + "' style='display: inline-block; padding: 10px 20px; background-color: #4CAF50; color: white; text-decoration: none; border-radius: 4px;'>重設密碼</a></p>"
                +
                "<p>或者，您可以複製以下鏈接到瀏覽器地址欄：</p>" +
                "<p><a href='" + resetUrl + "'>" + resetUrl + "</a></p>" +
                "<p>此鏈接將在30分鐘後過期。</p>" +
                "<p>如果您沒有請求重設密碼，請忽略此郵件。</p>" +
                "<p>謝謝，<br>健康管理系統團隊</p>" +
                "</div>";

        message.setContent(emailContent, "text/html; charset=UTF-8");
        Transport.send(message);
    }
}
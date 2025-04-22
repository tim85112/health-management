package com.healthmanagement.security.oauth2;

import com.healthmanagement.dao.member.UserDAO;
import com.healthmanagement.model.member.User;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.security.authentication.InternalAuthenticationServiceException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserService;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.user.DefaultOAuth2User;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.*;

/**
 * 自定義 OAuth2 使用者服務，處理 Google 登入邏輯
 */
@Service
public class CustomOAuth2UserService implements OAuth2UserService<OAuth2UserRequest, OAuth2User> {

    @Autowired
    private UserDAO userDAO;

    private final PasswordEncoder passwordEncoder;

    @Autowired
    public CustomOAuth2UserService(@Lazy PasswordEncoder passwordEncoder) {
        this.passwordEncoder = passwordEncoder;
    }

    /**
     * 實現 OAuth2UserService 的 loadUser 方法，處理 OAuth2 登入流程
     * 
     * @param userRequest OAuth2 使用者請求
     * @return OAuth2User 實例
     * @throws OAuth2AuthenticationException 認證異常
     */
    @Override
    public OAuth2User loadUser(OAuth2UserRequest userRequest) throws OAuth2AuthenticationException {
        OAuth2UserService<OAuth2UserRequest, OAuth2User> delegate = new org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService();
        // 使用標準服務獲取 OAuth2User
        OAuth2User oAuth2User = delegate.loadUser(userRequest);

        try {
            // 處理 OAuth2 使用者資訊
            return processOAuth2User(userRequest, oAuth2User);
        } catch (AuthenticationException ex) {
            throw ex;
        } catch (Exception ex) {
            // 捕獲任何其他異常並轉換為認證異常
            throw new InternalAuthenticationServiceException(ex.getMessage(), ex.getCause());
        }
    }

    /**
     * 處理 OAuth2 使用者資訊
     * 
     * @param userRequest OAuth2 使用者請求
     * @param oAuth2User  OAuth2 使用者
     * @return 處理後的 OAuth2User
     */
    private OAuth2User processOAuth2User(OAuth2UserRequest userRequest, OAuth2User oAuth2User) {
        // 從 OAuth2 使用者信息中獲取電子郵件
        String email = (String) oAuth2User.getAttributes().get("email");
        if (!StringUtils.hasText(email)) {
            throw new OAuth2AuthenticationException("Email not found from OAuth2 provider");
        }
        //// 從 OAuth2 使用者信息中獲取name
        String name = (String) oAuth2User.getAttributes().get("name");

        // 檢查電子郵件是否存在於資料庫
        Optional<User> userOptional = userDAO.findByEmail(email);
        User user;

        if (userOptional.isPresent()) {
            // 如果使用者已存在，更新登入時間
            user = userOptional.get();
            user.setLastLogin(LocalDateTime.now());
            userDAO.save(user);
        } else {
            // 如果使用者不存在，創建新使用者
            user = createUser(oAuth2User, userRequest);
        }

        // 創建 OAuth2User 的權限集合
        Collection<SimpleGrantedAuthority> authorities = Collections.singletonList(
                new SimpleGrantedAuthority(user.getRole()));

        // 創建使用者屬性對映
        Map<String, Object> attributes = new HashMap<>(oAuth2User.getAttributes());
        attributes.put("id", user.getId());
        attributes.put("role", user.getRole());
        attributes.put("name", user.getName());

        // 返回自定義 OAuth2User
        return new DefaultOAuth2User(
                authorities,
                attributes,
                "email");
    }

    /**
     * 創建新使用者
     * 
     * @param oAuth2User  OAuth2 使用者
     * @param userRequest OAuth2 使用者請求
     * @return 新創建的使用者
     */
    private User createUser(OAuth2User oAuth2User, OAuth2UserRequest userRequest) {
        // 從 OAuth2 使用者信息獲取基本資料
        String email = (String) oAuth2User.getAttributes().get("email");
        String name = (String) oAuth2User.getAttributes().get("name");

        // 創建隨機密碼（因為 OAuth2 登入不需要密碼，但我們的資料庫要求密碼欄位）
        String randomPassword = UUID.randomUUID().toString();
        String encodedPassword = passwordEncoder.encode(randomPassword);

        // 創建並儲存新使用者
        User user = User.builder()
                .email(email)
                .name(name)
                .passwordHash(encodedPassword)
                .role("user") // 預設角色為 user
                .userPoints(0) // 初始點數為 0
                .lastLogin(LocalDateTime.now())
                .build();

        return userDAO.save(user);
    }
}
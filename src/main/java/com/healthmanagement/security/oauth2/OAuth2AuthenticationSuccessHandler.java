package com.healthmanagement.security.oauth2;

import com.healthmanagement.util.JwtUtil;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.IOException;

/**
 * OAuth2 認證成功處理器
 * 處理 OAuth2 登入成功後的流程，重定向到前端並附帶 JWT token
 */
@Component
public class OAuth2AuthenticationSuccessHandler extends SimpleUrlAuthenticationSuccessHandler {

    @Autowired
    private JwtUtil jwtUtil;

    @Value("${app.frontend.url}")
    private String frontendUrl;

    /**
     * 處理認證成功事件
     * 
     * @param request        HTTP 請求
     * @param response       HTTP 回應
     * @param authentication 認證對象
     * @throws IOException      IO 異常
     * @throws ServletException Servlet 異常
     */
    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response,
            Authentication authentication) throws IOException, ServletException {
        // 獲取 OAuth2 使用者資訊
        OAuth2User oAuth2User = (OAuth2User) authentication.getPrincipal();
        String email = (String) oAuth2User.getAttributes().get("email");
        Integer userId = (Integer) oAuth2User.getAttributes().get("id");
        String role = (String) oAuth2User.getAttributes().get("role");

        // 生成 JWT token
        String token = jwtUtil.generateToken(email, role);

        // 添加日誌以確認值
        System.out.println("OAuth2 登入成功: email=" + email + ", userId=" + userId + ", role=" + role);
        System.out.println("生成的 token: " + token);

        // 構建重定向 URL，將 token 作為查詢參數
        String redirectUrl = UriComponentsBuilder.fromUriString(frontendUrl)
                .queryParam("token", token)
                .queryParam("userId", userId)
                .queryParam("email", email)
                .queryParam("role", role)
                .build().toUriString();

        System.out.println("重定向 URL: " + redirectUrl);

        // 重定向到前端應用
        getRedirectStrategy().sendRedirect(request, response, redirectUrl);
    }
}
package com.healthmanagement.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import com.healthmanagement.filter.JwtAuthenticationFilter;
import com.healthmanagement.security.oauth2.CustomOAuth2UserService;
import com.healthmanagement.security.oauth2.OAuth2AuthenticationSuccessHandler;
import org.springframework.web.util.UriComponentsBuilder;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import java.util.Arrays;
import java.util.List;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class WebSecurityConfig {

    @Autowired
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    @Autowired
    private CustomOAuth2UserService customOAuth2UserService;

    @Autowired
    private OAuth2AuthenticationSuccessHandler oAuth2AuthenticationSuccessHandler;

    @Value("${app.frontend.url:http://localhost:5173}")
    private String frontendUrl;

    // 在pom裝的security套件裡面，已經有BCryptPasswordEncoder，所以不需要自己再寫
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(Arrays.asList("http://localhost:5173"));
        configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(Arrays.asList("*"));
        configuration.setAllowCredentials(true);
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }

    @Bean
    @Primary
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(csrf -> csrf.disable())
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .authorizeHttpRequests(authorize -> authorize
                        .requestMatchers("/v3/api-docs/**",
                                "/v3/api-docs",
                                "/v3/api-docs.yaml",
                                "/swagger-ui/**",
                                "/swagger-ui.html",
                                "/swagger-resources/**",
                                "/swagger-ui/index.html",
                                "/webjars/**",
                                "/auth/**",
                                "/api/auth/**",
                                "/api/products/**",
                                "/api/cart/**",
                                "/api/newebpay/**",
                                "/api/order/**",
                                "/api/orders/*/payment/**",
                                "/users/**",
                                "/static-images/**",
                                "/login/oauth2/code/google", // 添加 OAuth2 重定向 URI
                                "/error")
                        .permitAll()
                        .requestMatchers("/api/users").hasAuthority("admin") // 獲取所有用戶僅限管理員
                        .requestMatchers("/api/users/{userId}").authenticated() // 獲取特定用戶需要登入，具體權限在Controller中控制
                        .requestMatchers("/api/users/{userId}/**").authenticated() // 用戶相關操作需要登入，具體權限在Controller中控制
                        .requestMatchers("/comments/post/**").authenticated() // 留言需登入
                        .requestMatchers("/comments/**").permitAll() // 查詢留言不用登入
                        .requestMatchers("/api/posts/**").authenticated()
                        .requestMatchers("/api/fitness/dashboard/stats").authenticated()
                        // 常規課程相關權限
                        .requestMatchers(HttpMethod.GET, "/api/courses/**").permitAll() // 只開放 GET 查詢課程信息
                        .requestMatchers("/api/courses/**").hasAuthority("admin") // 其他方法（POST/PUT/DELETE 課程管理）需要 admin
                        // 允許公開存取查詢常規課程狀態
                        .requestMatchers(HttpMethod.GET, "/api/enrollments/courses/*/is-full").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/enrollments/courses/*/count").permitAll()
                        // 常規課程報名記錄管理權限
                        .requestMatchers(HttpMethod.POST, "/api/enrollments/courses/*").hasAuthority("user") // 報名課程
                        .requestMatchers(HttpMethod.GET, "/api/enrollments/users/{userId}").authenticated() // 查詢特定使用者所有報名
                        .requestMatchers(HttpMethod.GET, "/api/enrollments/courses/{courseId}")
                        .hasAnyAuthority("admin", "coach") // 查詢特定課程所有報名
                        .requestMatchers(HttpMethod.GET, "/api/enrollments/users/*/courses/*/is-enrolled")
                        .authenticated() // 查詢是否已報名
                        .requestMatchers(HttpMethod.DELETE, "/api/enrollments/*").authenticated() // 取消報名
                        .requestMatchers(HttpMethod.PUT, "/api/enrollments/*/status").hasAnyAuthority("admin", "coach")
                        .requestMatchers(HttpMethod.GET, "/api/enrollments").hasAnyAuthority("admin", "coach")
                        .requestMatchers(HttpMethod.GET, "/api/enrollments/search/by-user-name")
                        .hasAnyAuthority("admin", "coach")
                        .requestMatchers("/api/enrollments/**").authenticated() // 可以保留作為安全網，如果還有其他未列出的 enrollment 端點
                        // 預約體驗的權限設定
                        .requestMatchers(HttpMethod.POST, "/api/trial-bookings/book").hasAnyAuthority("user", "guest")
                        // 其他體驗預約相關操作需要認證或特定權限
                        .requestMatchers(HttpMethod.GET, "/api/trial-bookings/courses/{courseId}")
                        .hasAnyAuthority("admin", "coach") // 查詢特定課程所有預約
                        .requestMatchers(HttpMethod.GET, "/api/trial-bookings//search/by-user-name")
                        .hasAnyAuthority("admin", "coach")
                        .requestMatchers(HttpMethod.GET, "/api/trial-bookings/users/{userId}").authenticated() // 查詢特定使用者所有預約
                        .requestMatchers(HttpMethod.GET, "/api/trial-bookings/{bookingId}").authenticated() // 查詢單個預約詳情
                        .requestMatchers(HttpMethod.DELETE, "/api/trial-bookings/{bookingId}").authenticated() // 取消預約
                                                                                                               // DELETE
                        .requestMatchers(HttpMethod.PUT, "/api/trial-bookings/{bookingId}/status")
                        .hasAnyAuthority("admin", "coach") // 更新狀態
                        .requestMatchers("/api/trial-bookings/**").authenticated() // 可以保留作為安全網
                        .anyRequest().authenticated())
                        .sessionManagement(session -> session
                        .sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                        // 配置匿名認證
                        .anonymous(anonymous -> anonymous
                        .authorities(List.of(new SimpleGrantedAuthority("guest")))) // 給予匿名用戶 'guest' 權限

                        // 配置 OAuth2 登入
                        .oauth2Login(oauth2 -> oauth2
                        .userInfoEndpoint(userInfo -> userInfo
                        .userService(customOAuth2UserService))
                        .successHandler(oAuth2AuthenticationSuccessHandler)
                        // 移除或註釋掉這一行
                        // .defaultSuccessUrl("http://localhost:5173/", true)
                        );


        // 啟用 JWT 過濾器
        http.addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    // 在OAuth2AuthenticationSuccessHandler中使用此方法构建重定向URL
    public String buildRedirectUrl(String token, String userId, String email, String role) {
        return UriComponentsBuilder.fromUriString(frontendUrl + "/oauth/callback")
                .queryParam("token", token)
                .queryParam("userId", userId)
                .queryParam("email", email)
                .queryParam("role", role)
                .build().toUriString();
    }
}
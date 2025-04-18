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
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import com.healthmanagement.filter.JwtAuthenticationFilter;

import java.util.Arrays;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class WebSecurityConfig {

    @Autowired
    private JwtAuthenticationFilter jwtAuthenticationFilter;

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
                                "/users/**")
                        .permitAll()
                        .requestMatchers("/api/users").hasAuthority("admin") // 獲取所有用戶僅限管理員
                        .requestMatchers("/api/users/{userId}").authenticated() // 獲取特定用戶需要登入，具體權限在Controller中控制
                        .requestMatchers("/api/users/{userId}/**").authenticated() // 用戶相關操作需要登入，具體權限在Controller中控制
                        .requestMatchers("/comments/post/**").authenticated() // 留言需登入
                        .requestMatchers("/comments/**").permitAll() // 查詢留言不用登入
                        .requestMatchers("/api/posts/**").authenticated()
                        .requestMatchers("/api/fitness/dashboard/stats").authenticated()
                        .requestMatchers(HttpMethod.GET, "/api/courses/**").permitAll() // 只開放 GET 查詢
                        .requestMatchers("/api/courses/**").hasAuthority("admin") // 其他方法（POST/PUT/DELETE）需要 admin
                        
                        // 允許公開存取 /error 路徑
                        .requestMatchers("/error").permitAll()
                        
                        // 允許公開存取查詢課程是否已滿和已報名人數
                        .requestMatchers("/api/enrollments/courses/*/is-full").permitAll()
                        .requestMatchers("/api/enrollments/courses/*/count").permitAll()

                        // EnrollmentController 的權限設定
                        .requestMatchers(HttpMethod.POST, "/api/enrollments/users/*/courses/*").hasAuthority("user") // 報名課程
                        .requestMatchers("/api/enrollments/{enrollmentId}").authenticated() // 取消報名、查詢報名ByID
                        .requestMatchers("/api/enrollments/users/{userId}").authenticated() // 查詢特定使用者的所有報名紀錄
                        .requestMatchers("/api/enrollments/courses/{courseId}").hasAnyAuthority("admin", "coach") // 查詢特定課程的所有報名紀錄
                        .requestMatchers(HttpMethod.GET, "/api/enrollments/users/*/courses/*/is-enrolled").authenticated() // 查詢特定使用者是否已報名特定課程
                        .requestMatchers("/api/enrollments/**").authenticated() // 確保所有 /api/enrollments/** 都需要登入 (作為最後的防線)
                        .anyRequest().authenticated())
                		.sessionManagement(session -> session
                        .sessionCreationPolicy(SessionCreationPolicy.STATELESS));

        // 啟用 JWT 過濾器
        http.addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}
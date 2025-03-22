package com.healthmanagement.config;

import com.healthmanagement.model.member.User;
import com.healthmanagement.service.member.UserService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

/**
 * 测试数据初始化器
 * 负责在应用启动时检查并加载测试数据
 * 只在非生产环境（dev, test）下执行
 */
@Configuration
public class TestDataInitializer {

    private static final Logger logger = LoggerFactory.getLogger(TestDataInitializer.class);

    private final UserService userService;

    @Autowired
    public TestDataInitializer(UserService userService) {
        this.userService = userService;
    }

    /**
     * 初始化测试数据
     * 使用 @Profile("!prod") 确保只在非生产环境执行
     * CommandLineRunner 接口确保在Spring Boot应用启动后执行
     */
    @Bean
    @Profile("!prod") // 只在非生产环境执行
    public CommandLineRunner initTestData() {
        return args -> {
            logger.info("开始初始化测试数据...");
            initTestUsers();
            // 可以在此添加更多测试数据初始化方法，如课程、健身计划等
            logger.info("测试数据初始化完成");
        };
    }

    /**
     * 初始化测试用户
     * 如果用户已存在（通过邮箱检查），则不会重新创建
     */
    private void initTestUsers() {
        // 检查测试用户是否存在
        if (!userService.existsByEmail("admin@example.com")) {
            User admin = User.builder()
                    .name("管理员")
                    .email("admin@example.com")
                    .passwordHash("admin123") // 实际存储时会被加密
                    .gender("M")
                    .bio("系统管理员")
                    .role("admin")
                    .userPoints(1000)
                    .build();
            userService.registerUser(admin);
            logger.info("已创建管理员测试账户: {}", admin.getEmail());
        } else {
            logger.info("管理员测试账户已存在，跳过创建");
        }

        if (!userService.existsByEmail("user@example.com")) {
            User normalUser = User.builder()
                    .name("普通用户")
                    .email("user@example.com")
                    .passwordHash("user123") // 实际存储时会被加密
                    .gender("F")
                    .bio("普通会员")
                    .role("user")
                    .userPoints(100)
                    .build();
            userService.registerUser(normalUser);
            logger.info("已创建普通用户测试账户: {}", normalUser.getEmail());
        } else {
            logger.info("普通用户测试账户已存在，跳过创建");
        }

        if (!userService.existsByEmail("coach@example.com")) {
            User coach = User.builder()
                    .name("教练")
                    .email("coach@example.com")
                    .passwordHash("coach123") // 实际存储时会被加密
                    .gender("M")
                    .bio("专业健身教练")
                    .role("coach")
                    .userPoints(500)
                    .build();
            userService.registerUser(coach);
            logger.info("已创建教练测试账户: {}", coach.getEmail());
        } else {
            logger.info("教练测试账户已存在，跳过创建");
        }
    }
}
package com.healthmanagement.config;

import com.healthmanagement.dao.member.UserDAO;
import com.healthmanagement.model.member.User;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component // 標記為 Spring 元件，啟動時自動執行
@Profile({ "dev", "test" }) // 僅在 dev 或 test 環境下執行（避免在 production 執行）
public class TestDataLoader implements CommandLineRunner {

    private final UserDAO userDAO;
    private final PasswordEncoder passwordEncoder;

    // 注入 UserDAO 和 PasswordEncoder
    @Autowired
    public TestDataLoader(UserDAO userDAO, PasswordEncoder passwordEncoder) {
        this.userDAO = userDAO;
        this.passwordEncoder = passwordEncoder;
    }

    // 複寫run方法
    @Override
    public void run(String... args) throws Exception {
        // 找出資料庫使用者數量
        long userCount = userDAO.count();
        System.out.println("當前資料庫使用者數量: " + userCount);

        // 如果少於20筆資料，則補充到20筆
        if (userCount < 20) {
            System.out.println("開始初始化測試使用者資料...");
            
            // 建立測試使用者
            List<User> testUsers = createTestUsers();
            
            // 儲存進資料庫
            userDAO.saveAll(testUsers);
            
            System.out.println("測試使用者資料初始化完成！共建立 " + testUsers.size() + " 位使用者");
        } else {
            System.out.println("資料庫已有足夠的使用者資料，跳過測試資料初始化");
        }

        // 顯示最終的資料庫使用者數量
        long finalCount = userDAO.count();
        System.out.println("目前資料庫總共有 " + finalCount + " 位使用者");
    }

    // 建立測試使用者清單
    private List<User> createTestUsers() {
        List<User> users = new ArrayList<>();

        // 使用工廠方法統一創建所有使用者

        // 創建管理員
        users.add(createUser("阿達", "ivan@example.com", "admin123", "M", "肌肉狂魔", "admin", 0));

        // 創建匿名登入者
        users.add(createUser("Guest", "guest@example.com", "guest123", "M", "匿名ABC", "guest", 0));

        // 創建一般使用者
        users.add(createUser("龍哥", "dragon@domain.com", "user123", "M", "woooooo", "user", 0));
        users.add(createUser("狗仔", "baobao@example.com", "user123", "M", "NILL", "user", 0));
        users.add(createUser("大哥2", "ivan2@example.com", "user123", "M", "NULL", "user", 0));
        
        // 創建測試用戶
        for (int i = 1; i <= 16; i++) {
            users.add(createUser(
                "測試用戶" + i,
                "test" + i + "@example.com",
                "password123",
                i % 2 == 0 ? "M" : "F",
                "測試用戶簡介" + i,
                "user",
                0 // 預設 coachId 為 0
            ));
        }

        // 自動生成額外 10 筆教練用戶
        for (int i = 1; i <= 10; i++) {
            users.add(createUser(
                "教練用戶" + (16 + i), // 延續編號，從 17 開始
                "coach" + i + "@example.com",
                "password123",
                i % 2 == 0 ? "F" : "M", // 稍微調整性別分配
                "專業教練簡介" + i,
                "coach",
                i // 設定 coachId，從 1 開始
            ));
        }

        return users;
    }

    // 工廠方法：統一建立 User 實例
    private User createUser(String name, String email, String password, String gender, String bio, String role,
            Integer points) {
        return User.builder()
                .name(name)
                .email(email)
                .passwordHash(passwordEncoder.encode(password)) // 密碼加密
                .gender(gender)
                .bio(bio)
                .role(role)
                .userPoints(points)
                .consecutiveLoginDays(0)
                .build();
    }
}

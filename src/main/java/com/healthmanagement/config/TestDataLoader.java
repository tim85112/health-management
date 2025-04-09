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

        // 添加一個環境變數控制是否強制重新初始化
        boolean forceReset = Boolean.parseBoolean(System.getProperty("app.db.force-reset", "false"));

        // 有資料就不重置
        if (userCount > 0 && !forceReset) {
            System.out.println("資料庫已有使用者資料，跳過測試資料初始化");
            return;
        }

        // 強制重置模式
        if (forceReset && userCount > 0) {
            System.out.println("強制重置模式：清除現有使用者資料...");
            userDAO.deleteAll();
        }

        System.out.println("開始初始化測試使用者資料...");

        // 建立測試使用者
        List<User> testUsers = createTestUsers();

        // 儲存進資料庫
        userDAO.saveAll(testUsers);

        System.out.println("測試使用者資料初始化完成！共建立 " + testUsers.size() + " 位使用者");
    }

    // 建立測試使用者清單
    private List<User> createTestUsers() {
        List<User> users = new ArrayList<>();

        // 使用工廠方法統一創建所有使用者

        // 創建管理員
        users.add(createUser("阿達", "ivan@example.com", "admin123", "M", "肌肉狂魔", "admin", 0));

        // 創建一般使用者
        users.add(createUser("龍哥", "dragon@domain.com", "user123", "M", "woooooo", "user", 0));
        users.add(createUser("狗仔", "baobao@example.com", "user123", "M", "NILL", "user", 0));
        users.add(createUser("大哥2", "ivan2@example.com", "user123", "M", "NULL", "user", 0));

        // 創建測試用戶
        users.add(createUser("測試用戶1", "test1@example.com", "password123", "M", "測試用戶簡介1", "user", 0));
        users.add(createUser("測試用戶2", "test2@example.com", "password123", "M", "測試用戶簡介2", "user", 0));
        users.add(createUser("測試用戶3", "test3@example.com", "password123", "M", "測試用戶簡介3", "user", 0));

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
                .build();
    }
}

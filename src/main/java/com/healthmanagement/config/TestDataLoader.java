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

        // 創建管理員
        users.add(createUser("YunDa", "yunda85112@gmail.com", "AAaa1234", "M", "健康管理系統管理員", "admin", 10000));
        users.add(createUser("Guest", "guest@example.com", "guest123", "M", "匿名ABC", "guest", 0));
        users.add(createUser("HuangJiaWei", "jiawei@health.com", "AAaa1234", "F", "營運經理", "admin", 0));
        users.add(createUser("王建國", "tech@health.com", "Tech@123", "M", "技術總監", "admin", 0));
        users.add(createUser("陳雅婷", "hr@health.com", "Hr@123", "F", "人力資源主管", "admin", 0));
        users.add(createUser("林志豪", "finance@health.com", "Finance@123", "M", "財務主管", "admin", 0));

        // 創建一般使用者
        users.add(createUser("WeiLong", "weilong@health.com", "AAaa1234", "F", "目標減重5公斤", "user", 0));
        users.add(createUser("陳聖亭", "shengting@health.com", "User@123", "M", "增肌訓練中", "user", 0));
        users.add(createUser("黃盛加", "shengjia@health.com", "User@123", "F", "準備馬拉松", "user", 0));
        users.add(createUser("張志強", "zhang@health.com", "User@123", "M", "改善體態", "user", 0));
        users.add(createUser("王美玲", "wang@health.com", "User@123", "F", "維持健康體態", "user", 0));

        // 新增15筆一般使用者
        users.add(createUser("YuQian", "yuqian@health.com", "AAaa1234", "M", "提升心肺功能", "user", 0));
        users.add(createUser("劉慎委", "shenwei@health.com", "User@123", "F", "改善柔軟度", "user", 0));
        users.add(createUser("鄭大偉", "zheng@health.com", "User@123", "M", "準備鐵人三項", "user", 0));
        users.add(createUser("周小玲", "zhou@health.com", "User@123", "F", "產後恢復訓練", "user", 0));
        users.add(createUser("蔡志豪", "cai@health.com", "User@123", "M", "提升運動表現", "user", 0));
        users.add(createUser("許美華", "xu@health.com", "User@123", "F", "改善姿勢問題", "user", 0));
        users.add(createUser("謝志強", "xie@health.com", "User@123", "M", "減脂增肌", "user", 0));
        users.add(createUser("洪小美", "hong@health.com", "User@123", "F", "提升核心力量", "user", 0));
        users.add(createUser("邱志明", "qiu@health.com", "User@123", "M", "改善肩頸問題", "user", 0));
        users.add(createUser("徐雅婷", "xu2@health.com", "User@123", "F", "準備半程馬拉松", "user", 0));
        users.add(createUser("孫大偉", "sun@health.com", "User@123", "M", "提升爆發力", "user", 0));
        users.add(createUser("朱小玲", "zhu@health.com", "User@123", "F", "改善平衡感", "user", 0));
        users.add(createUser("胡志豪", "hu@health.com", "User@123", "M", "提升耐力", "user", 0));
        users.add(createUser("高美華", "gao@health.com", "User@123", "F", "改善柔韌性", "user", 0));
        users.add(createUser("林志強", "lin2@health.com", "User@123", "M", "提升協調性", "user", 0));

        // 創建教練用戶
        users.add(createUser("XinLian", "xinlian@health.com", "AAaa1234", "M", "專業健身教練，專長：重量訓練", "coach", 1));
        users.add(createUser("林教練", "coach.lin@health.com", "Coach@123", "F", "瑜伽教練，專長：身心平衡", "coach", 2));
        users.add(createUser("黃教練", "coach.huang@health.com", "Coach@123", "M", "有氧教練，專長：心肺訓練", "coach", 3));
        users.add(createUser("張教練", "coach.zhang@health.com", "Coach@123", "F", "體態雕塑教練，專長：核心訓練", "coach", 4));
        users.add(createUser("王教練", "coach.wang@health.com", "Coach@123", "M", "功能性訓練教練，專長：運動表現提升", "coach", 5));

        users.add(createUser("陳教練", "coach.chen@health.com", "Coach@123", "M", "拳擊教練，專長：搏擊技巧", "coach", 6));
        users.add(createUser("李教練", "coach.li@health.com", "Coach@123", "F", "舞蹈教練，專長：韻律舞蹈", "coach", 7));
        users.add(createUser("吳教練", "coach.wu@health.com", "Coach@123", "M", "游泳教練，專長：自由式", "coach", 8));
        users.add(createUser("郭教練", "coach.guo@health.com", "Coach@123", "F", "皮拉提斯教練，專長：核心強化", "coach", 9));
        users.add(createUser("趙教練", "coach.zhao@health.com", "Coach@123", "M", "跑步教練，專長：長跑技巧", "coach", 10));
        users.add(createUser("沈教練", "coach.shen@health.com", "Coach@123", "F", "伸展教練，專長：柔韌度訓練", "coach", 11));
        users.add(createUser("楊教練", "coach.yang@health.com", "Coach@123", "M", "力量教練，專長：Olympic舉重", "coach", 12));
        users.add(createUser("徐教練", "coach.xu@health.com", "Coach@123", "F", "TRX懸吊訓練教練，專長：平衡訓練", "coach", 13));
        users.add(createUser("孫教練", "coach.sun@health.com", "Coach@123", "M", "籃球教練，專長：投籃技巧", "coach", 14));
        users.add(createUser("馬教練", "coach.ma@health.com", "Coach@123", "F", "營養教練，專長：飲食規劃", "coach", 15));

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

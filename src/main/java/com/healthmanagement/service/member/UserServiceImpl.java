package com.healthmanagement.service.member;

import com.healthmanagement.dao.member.UserDAO;
import com.healthmanagement.dto.member.AdminUpdateUserDTO;
import com.healthmanagement.dto.member.UpdateProfileDTO;
import com.healthmanagement.dto.member.UserDTO;
import com.healthmanagement.model.member.User;
import com.healthmanagement.util.JwtUtil;
import com.healthmanagement.service.fitness.AchievementService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Service
public class UserServiceImpl implements UserService {

    private final UserDAO userDAO;
    private final ApplicationContext applicationContext;
    private final AchievementService achievementService;
    private PasswordEncoder passwordEncoder;
    private JwtUtil jwtUtil;

    // 密碼驗證的正則表達式
    private static final Pattern PASSWORD_PATTERN = Pattern.compile("^(?=.*[a-z])(?=.*[A-Z]).{8,}$");

    @Autowired
    public UserServiceImpl(UserDAO userDAO, ApplicationContext applicationContext,
            AchievementService achievementService) {
        this.userDAO = userDAO;
        this.applicationContext = applicationContext;
        this.achievementService = achievementService;
    }

    @Autowired
    public void setPasswordEncoder(PasswordEncoder passwordEncoder) {
        this.passwordEncoder = passwordEncoder;
    }

    @Autowired
    public void setJwtUtil(JwtUtil jwtUtil) {
        this.jwtUtil = jwtUtil;
    }

    @Override
    public User registerUser(User user) {
        if (userDAO.existsByEmail(user.getEmail())) {
            throw new RuntimeException("Email already in use");
        }

        // 驗證密碼是否符合要求
        String rawPassword = user.getPasswordHash();
        if (rawPassword == null || rawPassword.length() < 8) {
            throw new RuntimeException("密碼必須至少包含8個字符");
        }
        if (!PASSWORD_PATTERN.matcher(rawPassword).matches()) {
            throw new RuntimeException("密碼必須包含至少一個大寫和一個小寫字母");
        }

        user.setPasswordHash(passwordEncoder.encode(rawPassword));
        if (user.getRole() == null) {
            user.setRole("user");
        }
        if (user.getUserPoints() == null) {
            user.setUserPoints(0);
        }
        return userDAO.save(user);
    }

    @Override
    public String loginUser(String email, String password) {
        User user = userDAO.findByEmail(email)
                .orElseThrow(() -> new BadCredentialsException("Invalid email or password"));
        if (!passwordEncoder.matches(password, user.getPasswordHash())) {
            throw new BadCredentialsException("Invalid email or password");
        }

        LocalDateTime now = LocalDateTime.now(ZoneId.of("Asia/Taipei"));
        LocalDate today = now.toLocalDate();
        LocalDate lastLoginDate = user.getLastLogin() != null ? user.getLastLogin().toLocalDate() : null;

        Integer consecutiveLoginDays = user.getConsecutiveLoginDays();
        if (consecutiveLoginDays == null) {
            consecutiveLoginDays = 0;
        }

        if (lastLoginDate != null && lastLoginDate.plusDays(1).isEqual(today)) {
            user.setConsecutiveLoginDays(consecutiveLoginDays + 1);
        } else {
            user.setConsecutiveLoginDays(1);
        }
        user.setLastLogin(now);
        userDAO.save(user);

        achievementService.checkAndAwardAchievements(user.getUserId(), "USER_LOGGED_IN",
                user.getConsecutiveLoginDays());

        return jwtUtil.generateToken(user.getEmail(), user.getRole());
    }

    @Override
    public Optional<User> getUserById(Integer userId) {
        return userDAO.findById(userId);
    }

    @Override
    public User updateUser(Integer userId, User userDetails) {
        User user = userDAO.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));
        if (userDetails.getName() != null) {
            user.setName(userDetails.getName());
        }
        if (userDetails.getEmail() != null && !userDetails.getEmail().equals(user.getEmail())) {
            if (userDAO.existsByEmail(userDetails.getEmail())) {
                throw new RuntimeException("Email already in use");
            }
            user.setEmail(userDetails.getEmail());
        }
        if (userDetails.getPasswordHash() != null) {
            // 驗證密碼是否符合要求
            String rawPassword = userDetails.getPasswordHash();
            if (rawPassword.length() < 8) {
                throw new RuntimeException("密碼必須至少包含8個字符");
            }
            if (!PASSWORD_PATTERN.matcher(rawPassword).matches()) {
                throw new RuntimeException("密碼必須包含至少一個大寫和一個小寫字母");
            }
            user.setPasswordHash(passwordEncoder.encode(userDetails.getPasswordHash()));
        }
        if (userDetails.getGender() != null) {
            user.setGender(userDetails.getGender());
        }
        if (userDetails.getBio() != null) {
            user.setBio(userDetails.getBio());
        }
        if (userDetails.getRole() != null) {
            user.setRole(userDetails.getRole());
        }
        if (userDetails.getUserPoints() != null) {
            user.setUserPoints(userDetails.getUserPoints());
        }
        return userDAO.save(user);
    }

    @Override
    public void deleteUser(Integer userId) {
        if (!userDAO.existsById(userId)) {
            throw new RuntimeException("User not found");
        }
        userDAO.deleteById(userId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<UserDTO> getAllUsers() {
        List<User> users = userDAO.findAll();

        return users.stream()
                .map(this::mapUserToUserDTO)
                .collect(Collectors.toList());
    }

    private UserDTO mapUserToUserDTO(User user) {
        UserDTO userDTO = new UserDTO();

        userDTO.setUserId(user.getId());
        userDTO.setName(user.getName());
        userDTO.setEmail(user.getEmail());
        userDTO.setGender(user.getGender());
        userDTO.setBio(user.getBio());
        userDTO.setRole(user.getRole());
        userDTO.setUserPoints(user.getUserPoints());
        return userDTO;
    }

    @Override
    public boolean existsByEmail(String email) {
        return userDAO.existsByEmail(email);
    }

    @Override
    public Optional<User> findByEmail(String email) {
        return userDAO.findByEmail(email);
    }

    @Override
    public List<User> findByName(String name) {
        return userDAO.findByName(name);
    }

    @Override
    public Optional<User> findById(Integer userId) {
        return userDAO.findById(userId);
    }

    @Override
    public List<User> getAllCoaches() {
        return userDAO.findByRole("coach");
    }

    /**
     * 更新用戶個人資料，僅允許更新姓名、性別、個人簡介和密碼
     * 
     * @param email            用戶郵箱
     * @param updateProfileDTO 包含要更新的資料
     * @return 更新後的用戶DTO
     */
    @Override
    public UserDTO updateUserProfile(String email, UpdateProfileDTO updateProfileDTO) {
        // 根據郵箱查找用戶
        User user = userDAO.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("用戶不存在"));

        // 更新姓名
        if (updateProfileDTO.getName() != null && !updateProfileDTO.getName().trim().isEmpty()) {
            user.setName(updateProfileDTO.getName());
        }

        // 更新性別
        if (updateProfileDTO.getGender() != null) {
            user.setGender(updateProfileDTO.getGender());
        }

        // 更新個人簡介
        if (updateProfileDTO.getBio() != null) {
            user.setBio(updateProfileDTO.getBio());
        }

        // 更新密碼（如果提供了新密碼）
        if (updateProfileDTO.getPassword() != null && !updateProfileDTO.getPassword().trim().isEmpty()) {
            // 如果提供了舊密碼，先驗證舊密碼是否正確
            if (updateProfileDTO.getOldPassword() != null && !updateProfileDTO.getOldPassword().trim().isEmpty()) {
                if (!passwordEncoder.matches(updateProfileDTO.getOldPassword(), user.getPasswordHash())) {
                    throw new RuntimeException("舊密碼不正確");
                }
            } else {
                throw new RuntimeException("更改密碼時必須提供舊密碼");
            }

            // 驗證新密碼是否符合要求
            String rawPassword = updateProfileDTO.getPassword();
            if (rawPassword.length() < 8) {
                throw new RuntimeException("密碼必須至少包含8個字符");
            }
            if (!PASSWORD_PATTERN.matcher(rawPassword).matches()) {
                throw new RuntimeException("密碼必須包含至少一個大寫和一個小寫字母");
            }

            // 加密並更新密碼
            user.setPasswordHash(passwordEncoder.encode(rawPassword));
        }

        // 保存更新後的用戶
        User updatedUser = userDAO.save(user);

        // 將實體轉換為DTO並返回
        return mapUserToUserDTO(updatedUser);
    }

    /**
     * 管理員更新用戶資料
     * 使用DTO避免反序列化問題
     */
    @Override
    public UserDTO adminUpdateUser(Integer userId, AdminUpdateUserDTO updateUserDTO) {
        // 查找用戶
        User user = userDAO.findById(userId)
                .orElseThrow(() -> new RuntimeException("用戶不存在"));

        // 更新基本資料
        if (updateUserDTO.getName() != null) {
            user.setName(updateUserDTO.getName());
        }

        // 只有當email不為空且與當前email不同時才檢查是否存在
        if (updateUserDTO.getEmail() != null && !updateUserDTO.getEmail().equals(user.getEmail())) {
            if (userDAO.existsByEmail(updateUserDTO.getEmail())) {
                throw new RuntimeException("電子郵件已被使用");
            }
            user.setEmail(updateUserDTO.getEmail());
        }

        // 更新其他欄位
        if (updateUserDTO.getGender() != null) {
            user.setGender(updateUserDTO.getGender());
        }

        if (updateUserDTO.getBio() != null) {
            user.setBio(updateUserDTO.getBio());
        }

        if (updateUserDTO.getRole() != null) {
            user.setRole(updateUserDTO.getRole());
        }

        if (updateUserDTO.getUserPoints() != null) {
            user.setUserPoints(updateUserDTO.getUserPoints());
        }

        // 如果提供了密碼，則更新密碼
        if (updateUserDTO.getPassword() != null && !updateUserDTO.getPassword().isEmpty()) {
            // 驗證密碼是否符合要求
            String rawPassword = updateUserDTO.getPassword();
            if (rawPassword.length() < 8) {
                throw new RuntimeException("密碼必須至少包含8個字符");
            }
            if (!PASSWORD_PATTERN.matcher(rawPassword).matches()) {
                throw new RuntimeException("密碼必須包含至少一個大寫和一個小寫字母");
            }

            // 加密並更新密碼
            user.setPasswordHash(passwordEncoder.encode(rawPassword));
        }

        // 保存更新
        User savedUser = userDAO.save(user);

        // 轉換為DTO返回
        return mapUserToUserDTO(savedUser);
    }
}
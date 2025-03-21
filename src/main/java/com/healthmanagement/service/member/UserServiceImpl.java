package com.healthmanagement.service.member;

import com.healthmanagement.dao.member.UserDAO;
import com.healthmanagement.model.member.User;
import com.healthmanagement.util.JwtUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class UserServiceImpl implements UserService {

    private final UserDAO userDAO;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;

    @Autowired
    public UserServiceImpl(UserDAO userDAO, PasswordEncoder passwordEncoder, JwtUtil jwtUtil) {
        this.userDAO = userDAO;
        this.passwordEncoder = passwordEncoder;
        this.jwtUtil = jwtUtil;
    }

    @Override
    public User registerUser(User user) {
        if (userDAO.existsByEmail(user.getEmail())) {
            throw new RuntimeException("Email already in use");
        }

        // 加密密码
        user.setPasswordHash(passwordEncoder.encode(user.getPasswordHash()));

        // 设置默认角色
        if (user.getRole() == null) {
            user.setRole("user");
        }

        // 设置默认积分
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

        // 更新用户信息
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
    public List<User> getAllUsers() {
        return userDAO.findAll();
    }

    @Override
    public boolean existsByEmail(String email) {
        return userDAO.existsByEmail(email);
    }

    @Override
    public Optional<User> findByEmail(String email) {
        return userDAO.findByEmail(email);
    }
}
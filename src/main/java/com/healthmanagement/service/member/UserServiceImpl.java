package com.healthmanagement.service.member;

import com.healthmanagement.dao.member.UserDAO;
import com.healthmanagement.model.member.User;
import com.healthmanagement.util.JwtUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import com.healthmanagement.service.fitness.AchievementService;

import java.time.LocalDateTime;
import java.time.LocalDate;
import java.time.ZoneId;

import java.util.List;
import java.util.Optional;

@Service
public class UserServiceImpl implements UserService {

	private final UserDAO userDAO;
	private final ApplicationContext applicationContext;
	private final AchievementService achievementService;
	private PasswordEncoder passwordEncoder;
	private JwtUtil jwtUtil;

	@Autowired
	public UserServiceImpl(UserDAO userDAO, ApplicationContext applicationContext,AchievementService achievementService) {
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
		user.setPasswordHash(passwordEncoder.encode(user.getPasswordHash()));
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
		User user = userDAO.findById(userId).orElseThrow(() -> new RuntimeException("User not found"));
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

	@Override
	public List<User> findByName(String name) {
		return userDAO.findByName(name);
	}

	@Override
	public Optional<User> findById(Integer userId) {
		return userDAO.findById(userId);
	}
}
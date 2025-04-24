package com.healthmanagement.service.member;

import com.healthmanagement.model.member.User;

import java.util.List;
import java.util.Optional;

public interface UserService {

	User registerUser(User user);

	String loginUser(String email, String password);

	Optional<User> getUserById(Integer userId);

	User updateUser(Integer userId, User userDetails);

	void deleteUser(Integer userId);

	List<User> getAllUsers();

	boolean existsByEmail(String email);

	Optional<User> findByEmail(String email);
	
	Optional<User> findById(Integer userId);

	List<User> findByName(String name);
	
	List<User> getAllCoaches();
}
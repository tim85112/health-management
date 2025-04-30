package com.healthmanagement.service.member;

import com.healthmanagement.dto.member.AdminUpdateUserDTO;
import com.healthmanagement.dto.member.UpdateProfileDTO;
import com.healthmanagement.dto.member.UserDTO;
import com.healthmanagement.model.member.User;

import java.util.List;
import java.util.Optional;

public interface UserService {

	User registerUser(User user);

	String loginUser(String email, String password);

	Optional<User> getUserById(Integer userId);

	User updateUser(Integer userId, User userDetails);

	void deleteUser(Integer userId);

	List<UserDTO> getAllUsers();

	boolean existsByEmail(String email);

	Optional<User> findByEmail(String email);

	Optional<User> findById(Integer userId);

	List<User> findByName(String name);

	List<User> getAllCoaches();

	/**
	 * 更新用戶個人資料
	 * 僅更新姓名、性別、個人簡介和密碼
	 * 
	 * @param email            用戶郵箱
	 * @param updateProfileDTO 包含要更新的資料
	 * @return 更新後的用戶DTO
	 */
	UserDTO updateUserProfile(String email, UpdateProfileDTO updateProfileDTO);

	/**
	 * 管理員更新用戶資料
	 * 
	 * @param userId        用戶ID
	 * @param updateUserDTO 更新數據
	 * @return 更新後的用戶DTO
	 */
	UserDTO adminUpdateUser(Integer userId, AdminUpdateUserDTO updateUserDTO);
}
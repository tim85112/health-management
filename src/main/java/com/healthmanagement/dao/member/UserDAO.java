package com.healthmanagement.dao.member;

import com.healthmanagement.model.member.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserDAO extends JpaRepository<User, Integer> {

	Optional<User> findByEmail(String email);

	boolean existsByEmail(String email);

	List<User> findByName(String name);
	
	List<User> findByRole(String role);
}
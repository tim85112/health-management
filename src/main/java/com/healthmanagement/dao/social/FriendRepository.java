package com.healthmanagement.dao.social;

import com.healthmanagement.model.social.Friend;
import com.healthmanagement.model.social.FriendId;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface FriendRepository extends JpaRepository<Friend, FriendId> {
    boolean existsByUserIdAndFriendId(Integer userId, Integer friendId);
    Optional<Friend> findByUserIdAndFriendId(Integer userId, Integer friendId);
    List<Friend> findAllByUserId(Integer userId);
}

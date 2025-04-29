package com.healthmanagement.dao.social;

import com.healthmanagement.model.social.Friend;
import com.healthmanagement.model.social.FriendId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface FriendRepository extends JpaRepository<Friend, FriendId> {
    boolean existsByUserIdAndFriendId(Integer userId, Integer friendId);
    Optional<Friend> findByUserIdAndFriendId(Integer userId, Integer friendId);
    List<Friend> findAllByUserId(Integer userId);
    
    @Query(value = "SELECT u.name, COUNT(*) FROM user_friend f JOIN users u ON f.user_id = u.user_id GROUP BY u.name ORDER BY COUNT(*) DESC", nativeQuery = true)
    List<Object[]> countFriendsByUser();
}

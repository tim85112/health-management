package com.healthmanagement.service.social;

import com.healthmanagement.dao.social.FriendRepository;
import com.healthmanagement.model.social.Friend;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class FriendServiceImpl implements FriendService {

    @Autowired
    private FriendRepository friendRepo;

    @Override
    public boolean addFriend(Integer userId, Integer friendId) {
        if (friendRepo.existsByUserIdAndFriendId(userId, friendId)) return false;

        Friend f1 = new Friend();
        f1.setUserId(userId);
        f1.setFriendId(friendId);
        f1.setCreatedAt(LocalDateTime.now());

        Friend f2 = new Friend();
        f2.setUserId(friendId);
        f2.setFriendId(userId);
        f2.setCreatedAt(LocalDateTime.now());

        friendRepo.save(f1);
        friendRepo.save(f2);
        return true;
    }

    @Override
    public boolean removeFriend(Integer userId, Integer friendId) {
        return friendRepo.findByUserIdAndFriendId(userId, friendId).map(f1 -> {
            friendRepo.delete(f1);
            friendRepo.findByUserIdAndFriendId(friendId, userId).ifPresent(friendRepo::delete);
            return true;
        }).orElse(false);
    }

    @Override
    public boolean isFriend(Integer userId, Integer friendId) {
        return friendRepo.existsByUserIdAndFriendId(userId, friendId);
    }

    @Override
    public List<Friend> getFriends(Integer userId) {
        return friendRepo.findAllByUserId(userId);
    }
}

package com.healthmanagement.service.social;

import com.healthmanagement.dao.social.FriendRepository;
import com.healthmanagement.dao.social.UserRepository;
import com.healthmanagement.dto.social.FriendDTO;
import com.healthmanagement.model.member.User;
import com.healthmanagement.model.social.Friend;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class FriendServiceImpl implements FriendService {

    @Autowired
    private FriendRepository friendRepo;
    
    @Autowired
    private UserRepository userRepo;

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
    public List<FriendDTO> getFriends(Integer userId) {
        List<Friend> rawFriends = friendRepo.findAllByUserId(userId);

        return rawFriends.stream()
                .map(f -> {
                    User friendUser = userRepo.findById(f.getFriendId()).orElse(null);
                    if (friendUser == null) {
                        System.err.println("找不到使用者 ID: " + f.getFriendId());
                    }
                    return new FriendDTO(
                        f.getFriendId(),
                        friendUser != null ? friendUser.getName() : "未知使用者"
                    );
                })
                .collect(Collectors.toList());
    }
}

package com.healthmanagement.service.social;

import com.healthmanagement.model.social.Friend;

import java.util.List;

public interface FriendService {
    boolean addFriend(Integer userId, Integer friendId);
    boolean removeFriend(Integer userId, Integer friendId);
    boolean isFriend(Integer userId, Integer friendId);
    List<Friend> getFriends(Integer userId);
}

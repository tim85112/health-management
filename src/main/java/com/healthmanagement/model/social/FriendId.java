package com.healthmanagement.model.social;

import java.io.Serializable;
import java.util.Objects;

public class FriendId implements Serializable {
    private Integer userId;
    private Integer friendId;

    public FriendId() {}

    public FriendId(Integer userId, Integer friendId) {
        this.userId = userId;
        this.friendId = friendId;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof FriendId)) return false;
        FriendId that = (FriendId) o;
        return Objects.equals(userId, that.userId) && Objects.equals(friendId, that.friendId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(userId, friendId);
    }
}

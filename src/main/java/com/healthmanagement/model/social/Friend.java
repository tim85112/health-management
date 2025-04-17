package com.healthmanagement.model.social;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Getter
@Setter
@IdClass(FriendId.class)
@Table(name = "user_friend")
public class Friend {

    @Id
    @Column(name = "user_id")
    private Integer userId;

    @Id
    @Column(name = "friend_id")
    private Integer friendId;

    @Column(name = "created_at")
    private LocalDateTime createdAt;
}

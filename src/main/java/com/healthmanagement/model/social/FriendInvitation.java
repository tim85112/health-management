package com.healthmanagement.model.social;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "friend_invitation", uniqueConstraints = @UniqueConstraint(columnNames = {"inviter_id", "invitee_id"}))
public class FriendInvitation {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "inviter_id")
    private Integer inviterId;

    @Column(name = "invitee_id")
    private Integer inviteeId;

    private String status = "PENDING"; // 預設為 PENDING

    @Column(name = "created_at")
    private LocalDateTime createdAt = LocalDateTime.now();

}

package com.healthmanagement.model.social;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Getter
@Setter
@Table(name = "training_invitation")
public class TrainingInvitation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "sender_id", nullable = false)
    private Integer senderId;

    @Column(name = "receiver_id", nullable = false)
    private Integer receiverId;

    @Column(name = "message")
    private String message;

    @Column(name = "status", nullable = false)
    private String status = "pending"; // default: pending

    @Column(name = "sent_at")
    private LocalDateTime sentAt;
}

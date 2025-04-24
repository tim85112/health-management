package com.healthmanagement.dto.social;

import java.time.LocalDateTime;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class TrainingInvitationDTO {

    private Integer id;
    private String senderName;
    private String message;
    private String status;
    private LocalDateTime sentAt;  
    private String receiverName;
}

package com.healthmanagement.dto.social;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class FriendInvitationDTO {
    private Integer id;
    private Integer inviterId;
    private String inviterName;
    private String status;
}

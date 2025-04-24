package com.healthmanagement.service.social;

import com.healthmanagement.dto.social.TrainingInvitationDTO;
import com.healthmanagement.model.social.TrainingInvitation;

import java.util.List;

public interface TrainingInvitationService {
    TrainingInvitation sendInvitation(Integer senderId, Integer receiverId, String message);
    boolean respondToInvitation(Integer invitationId, String status);
    List<TrainingInvitationDTO> getSentInvitations(Integer senderId);
    List<TrainingInvitationDTO> getReceivedInvitations(Integer receiverId);
}

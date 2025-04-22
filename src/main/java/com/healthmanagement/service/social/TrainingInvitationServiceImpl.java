package com.healthmanagement.service.social;

import com.healthmanagement.dao.social.TrainingInvitationRepository;
import com.healthmanagement.dto.social.TrainingInvitationDTO;
import com.healthmanagement.model.member.User;
import com.healthmanagement.model.social.TrainingInvitation;
import com.healthmanagement.service.member.UserService;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class TrainingInvitationServiceImpl implements TrainingInvitationService {

    @Autowired
    private TrainingInvitationRepository repo;
    
    @Autowired
    private FriendService friendService;
    
    @Autowired
    private UserActivityService userActivityService;
    
    @Autowired
    private UserService userService;
    
    @Override
    public TrainingInvitation sendInvitation(Integer senderId, Integer receiverId, String message) {
        // ✅ 檢查是否為好友
        if (!friendService.isFriend(senderId, receiverId)) {
            throw new RuntimeException("只能邀請好友進行訓練！");
        }
        TrainingInvitation invite = new TrainingInvitation();
        invite.setSenderId(senderId);
        invite.setReceiverId(receiverId);
        invite.setMessage(message);
        invite.setStatus("pending");
        invite.setSentAt(LocalDateTime.now());

        TrainingInvitation saved = repo.save(invite);

        // ✅ 記錄活動：發送訓練邀請
        userActivityService.logActivity(senderId, "invite", saved.getId());

        return saved;
    }

    @Override
    public boolean respondToInvitation(Integer id, String status) {
        return repo.findById(id).map(invite -> {
            invite.setStatus(status); // accepted / rejected
            repo.save(invite);
            return true;
        }).orElse(false);
    }

    @Override
    public List<TrainingInvitationDTO> getSentInvitations(Integer senderId) {
        List<TrainingInvitation> invitations = repo.findBySenderId(senderId);
        return invitations.stream().map(invite -> {
            TrainingInvitationDTO dto = new TrainingInvitationDTO();
            dto.setId(invite.getId());
            dto.setMessage(invite.getMessage());
            dto.setStatus(invite.getStatus());
            dto.setSentAt(invite.getSentAt());

            // 查詢接收者名稱（邀請對象）
            User receiver = userService.findById(invite.getReceiverId()).orElse(null);
            dto.setReceiverName(receiver != null ? receiver.getName() : "未知對象");

            return dto;
        }).collect(Collectors.toList());
    }

    @Override
    public List<TrainingInvitationDTO> getReceivedInvitations(Integer receiverId) {
        List<TrainingInvitation> invitations = repo.findByReceiverId(receiverId);
        return invitations.stream().map(invite -> {
            TrainingInvitationDTO dto = new TrainingInvitationDTO();
            dto.setId(invite.getId());
            dto.setMessage(invite.getMessage());
            dto.setStatus(invite.getStatus());
            dto.setSentAt(invite.getSentAt());

         // 只取發送者名稱
            User sender = userService.findById(invite.getSenderId()).orElse(null);
            dto.setSenderName(sender != null ? sender.getName() : "未知");
            
            return dto;
        }).collect(Collectors.toList());
    }
}

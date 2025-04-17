package com.healthmanagement.service.social;

import com.healthmanagement.dao.social.TrainingInvitationRepository;
import com.healthmanagement.model.social.TrainingInvitation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class TrainingInvitationServiceImpl implements TrainingInvitationService {

    @Autowired
    private TrainingInvitationRepository repo;
    
    @Autowired
    private FriendService friendService;
    
    @Autowired
    private UserActivityService userActivityService;
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
    public List<TrainingInvitation> getSentInvitations(Integer senderId) {
        return repo.findBySenderId(senderId);
    }

    @Override
    public List<TrainingInvitation> getReceivedInvitations(Integer receiverId) {
        return repo.findByReceiverId(receiverId);
    }
}

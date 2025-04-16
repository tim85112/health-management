package com.healthmanagement.dao.social;

import com.healthmanagement.model.social.TrainingInvitation;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface TrainingInvitationRepository extends JpaRepository<TrainingInvitation, Integer> {
    List<TrainingInvitation> findByReceiverId(Integer receiverId);
    List<TrainingInvitation> findBySenderId(Integer senderId);
}

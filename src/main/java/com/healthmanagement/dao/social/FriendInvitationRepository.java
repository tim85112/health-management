package com.healthmanagement.dao.social;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.healthmanagement.model.social.FriendInvitation;

@Repository
public interface FriendInvitationRepository extends JpaRepository<FriendInvitation, Integer> {
    Optional<FriendInvitation> findByInviterIdAndInviteeId(Integer inviterId, Integer inviteeId);
    List<FriendInvitation> findByInviteeIdAndStatus(Integer inviteeId, String status);
}

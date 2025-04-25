package com.healthmanagement.controller.social;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.healthmanagement.dao.social.FriendInvitationRepository;
import com.healthmanagement.dao.social.FriendRepository;
import com.healthmanagement.model.social.Friend;
import com.healthmanagement.model.social.FriendInvitation;
import com.healthmanagement.service.member.UserService;

import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@RequestMapping("/api/friend-invitations")
@Tag(name = "好友邀請", description = "好友邀請 API")
public class FriendInvitationController {

    @Autowired
    private FriendInvitationRepository repo;

    @Autowired
    private FriendRepository friendRepo;

    @Autowired
    private UserService userService;

    private Integer getLoginUserId() {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        return userService.findByEmail(email).orElseThrow().getUserId();
    }

    // 發送邀請
    @PostMapping("/{inviteeId}")
    public ResponseEntity<?> sendInvitation(@PathVariable Integer inviteeId) {
        Integer inviterId = getLoginUserId();
        if (repo.findByInviterIdAndInviteeId(inviterId, inviteeId).isPresent()) {
            return ResponseEntity.badRequest().body("已送出邀請");
        }
        FriendInvitation invitation = new FriendInvitation();
        invitation.setInviterId(inviterId);
        invitation.setInviteeId(inviteeId);
        repo.save(invitation);
        return ResponseEntity.ok("邀請已送出");
    }

    // 接受邀請
    @PostMapping("/{invitationId}/accept")
    public ResponseEntity<?> acceptInvitation(@PathVariable Integer invitationId) {
        FriendInvitation invitation = repo.findById(invitationId).orElseThrow();
        invitation.setStatus("ACCEPTED");
        repo.save(invitation);

        // 雙向加入好友表
        Friend f1 = new Friend(invitation.getInviterId(), invitation.getInviteeId());
        Friend f2 = new Friend(invitation.getInviteeId(), invitation.getInviterId());
        friendRepo.save(f1);
        friendRepo.save(f2);

        return ResponseEntity.ok("已接受好友邀請");
    }

    // 拒絕邀請
    @PostMapping("/{invitationId}/reject")
    public ResponseEntity<?> rejectInvitation(@PathVariable Integer invitationId) {
        FriendInvitation invitation = repo.findById(invitationId).orElseThrow();
        invitation.setStatus("REJECTED");
        repo.save(invitation);
        return ResponseEntity.ok("已拒絕邀請");
    }

    // 查看我的邀請列表
    @GetMapping("/received")
    public ResponseEntity<List<FriendInvitation>> getReceivedInvites() {
        Integer userId = getLoginUserId();
        return ResponseEntity.ok(repo.findByInviteeIdAndStatus(userId, "PENDING"));
    }
}


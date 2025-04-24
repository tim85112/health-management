package com.healthmanagement.controller.social;

import com.healthmanagement.dto.social.TrainingInvitationDTO;
import com.healthmanagement.model.social.TrainingInvitation;
import com.healthmanagement.service.member.UserService;
import com.healthmanagement.service.social.TrainingInvitationService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/training-invitations")
@Tag(name = "訓練邀請", description = "訓練邀請管理API")
public class TrainingInvitationController {

    @Autowired
    private TrainingInvitationService invitationService;

    @Autowired
    private UserService userService;

    private Integer getLoginUserId() {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        return userService.findByEmail(email).orElseThrow().getUserId();
    }

    @PostMapping("/invite")
    @Operation(summary = "訓練邀請")
    public ResponseEntity<TrainingInvitation> sendInvitation(@RequestBody TrainingInviteRequest req) {
        Integer senderId = getLoginUserId();
        return ResponseEntity.ok(invitationService.sendInvitation(senderId, req.getReceiverId(), req.getMessage()));
    }
    
    public static class TrainingInviteRequest {
        private Integer receiverId;
        private String message;
        public Integer getReceiverId() { return receiverId; }
        public void setReceiverId(Integer receiverId) { this.receiverId = receiverId; }
        public String getMessage() { return message; }
        public void setMessage(String message) { this.message = message; }
    }

    @PutMapping("/respond/{id}")
    @Operation(summary = "回覆邀請")
    public ResponseEntity<String> respond(@PathVariable Integer id, @RequestParam String status) {
        return invitationService.respondToInvitation(id, status)
                ? ResponseEntity.ok("Updated")
                : ResponseEntity.badRequest().body("Not found");
    }

    @GetMapping("/sent")
    @Operation(summary = "查詢ID發送邀請紀錄")
    public ResponseEntity<List<TrainingInvitationDTO>> mySentInvites() {
        return ResponseEntity.ok(invitationService.getSentInvitations(getLoginUserId()));
    }

    @GetMapping("/received")
    @Operation(summary = "查詢ID收到邀請紀錄")
    public ResponseEntity<List<TrainingInvitationDTO>> myReceivedInvites() {
        return ResponseEntity.ok(invitationService.getReceivedInvitations(getLoginUserId()));
    }
}

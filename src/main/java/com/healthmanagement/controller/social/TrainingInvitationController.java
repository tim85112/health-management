package com.healthmanagement.controller.social;

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
@RequestMapping("/api/training")
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

    @PostMapping("/invite/{receiverId}")
    @Operation(summary = "訓練邀請")
    public ResponseEntity<TrainingInvitation> sendInvitation(@PathVariable Integer receiverId,
                                                             @RequestBody(required = false) String message) {
        Integer senderId = getLoginUserId();
        return ResponseEntity.ok(invitationService.sendInvitation(senderId, receiverId, message));
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
    public ResponseEntity<List<TrainingInvitation>> mySentInvites() {
        return ResponseEntity.ok(invitationService.getSentInvitations(getLoginUserId()));
    }

    @GetMapping("/received")
    @Operation(summary = "查詢ID收到邀請紀錄")
    public ResponseEntity<List<TrainingInvitation>> myReceivedInvites() {
        return ResponseEntity.ok(invitationService.getReceivedInvitations(getLoginUserId()));
    }
}

package com.healthmanagement.controller.social;

import com.healthmanagement.dto.social.FriendDTO;
import com.healthmanagement.service.member.UserService;
import com.healthmanagement.service.social.FriendService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/friends")
@Tag(name = "好友狀態", description = "好友狀態管理API")
public class FriendController {

    @Autowired
    private FriendService friendService;

    @Autowired
    private UserService userService;

    private Integer getLoginUserId() {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        return userService.findByEmail(email).orElseThrow().getUserId(); // 取 ID 而不是整個物件
    }

    @PostMapping("/{friendId}")
    @Operation(summary = "新增好友")
    public ResponseEntity<String> addFriend(@PathVariable Integer friendId) {
        Integer myId = getLoginUserId();
        return friendService.addFriend(myId, friendId)
                ? ResponseEntity.ok("Friend added")
                : ResponseEntity.badRequest().body("Already friends");
    }

    @DeleteMapping("/{friendId}")
    @Operation(summary = "刪除好友")
    public ResponseEntity<String> removeFriend(@PathVariable Integer friendId) {
        Integer myId = getLoginUserId();
        return friendService.removeFriend(myId, friendId)
                ? ResponseEntity.ok("Friend removed")
                : ResponseEntity.badRequest().body("Not friends");
    }

    @GetMapping
    @Operation(summary = "顯示全部好友")
    public ResponseEntity<List<Map<String, Object>>> getFriends() {
        Integer myId = getLoginUserId();
        List<FriendDTO> friends = friendService.getFriends(myId);

        List<Map<String, Object>> result = new ArrayList<>();
        for (FriendDTO f : friends) {
            Map<String, Object> map = new HashMap<>();
            map.put("friendId", f.getFriendId());
            map.put("friendName", f.getName()); 
            result.add(map);
        }

        return ResponseEntity.ok(result);
    }
}

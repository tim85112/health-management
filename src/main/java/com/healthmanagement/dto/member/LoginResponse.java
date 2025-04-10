package com.healthmanagement.dto.member;

import com.healthmanagement.model.member.User;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class LoginResponse {
    private String token;
    private String role;
    private User user;
    
    public LoginResponse(String token, String role) {
        this.token = token;
        this.role = role;
    }
}
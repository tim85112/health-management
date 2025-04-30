package com.healthmanagement.dto.member;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserDTO {

	private Integer userId;
    private String name;
    private String email;
    private String gender;
    private String bio;
    private String role;
    private Integer userPoints;
    // private LocalDateTime lastLogin; // 如果需要也可以包含最後登入時間
}
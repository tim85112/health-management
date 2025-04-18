package com.healthmanagement.dto.member;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class RegisterRequest {

    @NotBlank(message = "Name is required")
    @Size(min = 2, max = 50, message = "Name must be between 2 and 50 characters")
    private String name;

    @NotBlank(message = "Email is required")
    @Email(message = "Email should be valid")
    private String email;

    @NotBlank(message = "Password is required")
    @Size(min = 8, message = "密碼必須至少包含8個字符")
    @Pattern(regexp = "^(?=.*[a-z])(?=.*[A-Z]).*$", message = "密碼必須包含至少一個大寫和一個小寫字母")
    private String password;

    @Pattern(regexp = "^[MFO]$", message = "Gender must be M, F, or O")
    private String gender;

    private String bio;
}
package com.example.lock.controller.dto;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class LoginResponse {
    private String token;
    private Long userId;
    private String fullName;
    private String email;
    private String role;
}

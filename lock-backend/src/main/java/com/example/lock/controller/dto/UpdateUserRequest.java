package com.example.lock.controller.dto;

import com.example.lock.user.entity.Role;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UpdateUserRequest {
    @NotBlank
    private String fullName;
    private String phone;
    @NotNull
    private Role role;
    private boolean active = true;
}

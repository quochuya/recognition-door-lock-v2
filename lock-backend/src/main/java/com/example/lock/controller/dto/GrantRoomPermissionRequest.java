package com.example.lock.controller.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
public class GrantRoomPermissionRequest {
    @NotNull
    private Long roomId;
    private LocalDateTime expiresAt; // null = vô thời hạn
}

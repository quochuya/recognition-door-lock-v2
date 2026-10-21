package com.example.lock.controller.dto;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;

@Getter
@Setter
public class UpdateCardRequest {
    private Long ownerId; // null = giữ nguyên chủ thẻ, chỉ đổi hạn dùng
    private LocalDate expiresAt;
}

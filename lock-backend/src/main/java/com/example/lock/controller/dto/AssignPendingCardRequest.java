package com.example.lock.controller.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;
import java.util.List;

@Getter
@Setter
public class AssignPendingCardRequest {
    @NotNull
    private Long userId;
    private LocalDate expiresAt;
    private List<Long> roomIds;
}

package com.example.lock.controller.dto;

import com.example.lock.access.result.AccessResult;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class LockCommandResponse {
    private boolean success;
    private String message;

    public static LockCommandResponse from(AccessResult result) {
        return LockCommandResponse.builder().success(result.isAllowed()).message(result.getReason()).build();
    }
}

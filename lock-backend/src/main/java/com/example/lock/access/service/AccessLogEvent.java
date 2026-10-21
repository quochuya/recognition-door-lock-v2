package com.example.lock.access.service;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class AccessLogEvent {
    private String deviceCode;
    private String roomLabel;
    private String userName;
    private String method;
    private boolean success;
    private String reason;
    private LocalDateTime occurredAt;
    @Builder.Default
    private boolean offline = false;
}

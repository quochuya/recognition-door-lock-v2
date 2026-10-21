package com.example.lock.controller.dto;

import com.example.lock.access.entity.AccessLog;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class AccessLogDto {
    private Long id;
    private String userName;
    private String method;
    private String deviceCode;
    private String buildingName;
    private Integer roomNumber;
    private boolean success;
    private String denyReason;
    private boolean offline;
    private LocalDateTime occurredAt;

    public static AccessLogDto from(AccessLog log) {
        return AccessLogDto.builder()
                .id(log.getId())
                .userName(log.getUser() != null ? log.getUser().getFullName() : "Không xác định")
                .method(log.getMethod().name())
                .deviceCode(log.getLockDevice().getDeviceCode())
                .buildingName(log.getLockDevice().getRoom().getFloor().getBuilding().getName())
                .roomNumber(log.getLockDevice().getRoom().getRoomNumber())
                .success(log.isSuccess())
                .denyReason(log.getDenyReason())
                .offline(log.isOffline())
                .occurredAt(log.getOccurredAt())
                .build();
    }
}

package com.example.lock.controller.dto;

import com.example.lock.access.entity.RoomPermission;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class RoomPermissionDto {
    private Long roomId;
    private String buildingName;
    private Integer roomNumber;
    private LocalDateTime grantedAt;
    private LocalDateTime expiresAt;

    public static RoomPermissionDto from(RoomPermission p) {
        return RoomPermissionDto.builder()
                .roomId(p.getRoom().getId())
                .buildingName(p.getRoom().getFloor().getBuilding().getName())
                .roomNumber(p.getRoom().getRoomNumber())
                .grantedAt(p.getGrantedAt())
                .expiresAt(p.getExpiresAt())
                .build();
    }
}

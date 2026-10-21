package com.example.lock.controller.dto;

import com.example.lock.room.entity.LockDevice;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class DeviceDto {
    private Long id;
    private String deviceCode;
    private String buildingName;
    private Integer floorLevel;
    private Integer roomNumber;
    private Integer batteryPercent;
    private Integer signalPercent;
    private String firmwareVersion;
    private LockDevice.LockState lockState;
    private LockDevice.ConnectionStatus connectionStatus;
    private LocalDateTime lastSeenAt;

    public static DeviceDto from(LockDevice d) {
        return DeviceDto.builder()
                .id(d.getId())
                .deviceCode(d.getDeviceCode())
                .buildingName(d.getRoom().getFloor().getBuilding().getName())
                .floorLevel(d.getRoom().getFloor().getLevel())
                .roomNumber(d.getRoom().getRoomNumber())
                .batteryPercent(d.getBatteryPercent())
                .signalPercent(d.getSignalPercent())
                .firmwareVersion(d.getFirmwareVersion())
                .lockState(d.getLockState())
                .connectionStatus(d.getConnectionStatus())
                .lastSeenAt(d.getLastSeenAt())
                .build();
    }
}

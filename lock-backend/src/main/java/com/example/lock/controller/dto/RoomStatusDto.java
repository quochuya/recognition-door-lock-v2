package com.example.lock.controller.dto;

import com.example.lock.room.entity.LockDevice;
import com.example.lock.room.entity.Room;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class RoomStatusDto {
    private Long roomId;
    private Integer roomNumber;
    private Integer floorLevel;
    private Long lockDeviceId;
    private String deviceCode;
    private LockDevice.LockState lockState;
    private LockDevice.ConnectionStatus connectionStatus;
    private Integer batteryPercent;

    public static RoomStatusDto from(Room room, LockDevice device) {
        return RoomStatusDto.builder()
                .roomId(room.getId())
                .roomNumber(room.getRoomNumber())
                .floorLevel(room.getFloor().getLevel())
                .lockDeviceId(device != null ? device.getId() : null)
                .deviceCode(device != null ? device.getDeviceCode() : null)
                .lockState(device != null ? device.getLockState() : null)
                .connectionStatus(device != null ? device.getConnectionStatus() : LockDevice.ConnectionStatus.OFFLINE)
                .batteryPercent(device != null ? device.getBatteryPercent() : null)
                .build();
    }
}

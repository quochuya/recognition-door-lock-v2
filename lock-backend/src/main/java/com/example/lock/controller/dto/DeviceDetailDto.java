package com.example.lock.controller.dto;

import com.example.lock.access.entity.AccessLog;
import com.example.lock.room.entity.LockDevice;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDate;
import java.util.List;

@Getter
@Builder
public class DeviceDetailDto {
    private DeviceDto device;
    private LocalDate installedAt;
    private List<AccessLogDto> recentLogs;

    public static DeviceDetailDto from(LockDevice d, List<AccessLog> logs) {
        return DeviceDetailDto.builder()
                .device(DeviceDto.from(d))
                .installedAt(d.getInstalledAt() != null ? d.getInstalledAt().toLocalDate() : null)
                .recentLogs(logs.stream().map(AccessLogDto::from).toList())
                .build();
    }
}

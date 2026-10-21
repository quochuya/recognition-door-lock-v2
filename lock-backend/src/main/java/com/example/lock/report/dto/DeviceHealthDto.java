package com.example.lock.report.dto;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class DeviceHealthDto {
    private long batteryGood;   // >= 50%
    private long batteryMedium; // 20-49%
    private long batteryLow;    // < 20%
    private long online;
    private long offline;
}

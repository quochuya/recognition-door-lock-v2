package com.example.lock.report.dto;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class ReportSummaryDto {
    private long totalUnlock;       // = successCount
    private long deniedCount;
    private double successRate;     // %
    private long lowBatteryDevices; // pin < 20%
}

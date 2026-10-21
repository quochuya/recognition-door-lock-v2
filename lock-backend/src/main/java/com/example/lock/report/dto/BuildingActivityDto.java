package com.example.lock.report.dto;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class BuildingActivityDto {
    private String buildingName;
    private long activityCount;
    private long offlineDeviceCount;
}

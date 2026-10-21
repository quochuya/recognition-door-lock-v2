package com.example.lock.report.dto;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDate;

@Getter
@Builder
public class DailyActivityDto {
    private LocalDate date;
    private long successCount;
    private long deniedCount;
}

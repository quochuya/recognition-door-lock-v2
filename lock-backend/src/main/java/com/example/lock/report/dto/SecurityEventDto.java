package com.example.lock.report.dto;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class SecurityEventDto {
    private String ruleKey;
    private String label; // mô tả người đọc được, map từ ruleKey
    private long count;
}

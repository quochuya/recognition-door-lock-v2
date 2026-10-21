package com.example.lock.report.dto;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class TopRoomDto {
    private String roomLabel;
    private long count;
}

package com.example.lock.controller.dto;

import com.example.lock.room.entity.Floor;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class FloorDto {
    private Long id;
    private Integer level;

    public static FloorDto from(Floor f) {
        return FloorDto.builder().id(f.getId()).level(f.getLevel()).build();
    }
}

package com.example.lock.controller.dto;

import com.example.lock.room.entity.Building;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class BuildingDto {
    private Long id;
    private String name;
    private String address;

    public static BuildingDto from(Building b) {
        return BuildingDto.builder().id(b.getId()).name(b.getName()).address(b.getAddress()).build();
    }
}
